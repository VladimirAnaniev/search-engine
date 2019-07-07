package searchengine

import java.io.File
import java.util.concurrent.Executors

import searchengine.html.HtmlUtils
import searchengine.http.{AsyncHttpClient, HttpUtils}
import searchengine.math.Monoid._
import searchengine.math.Monoid.ops._
import searchengine.processors.{BrokenLinkDetector, FileOutput, LinkReferences, LinkReferencesMap, WordCount, WordCounter, WordOccurence, WordOccurencerCounter}
import javax.management.InvalidApplicationException
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source

class CrawlerTest extends FlatSpec with Matchers with ScalaFutures {
  val mockHttpClient = new MockHttpClient
  val testSpidey = new Spidey(mockHttpClient)

  "response from MockHttpClient" should "be html resource containing valid http links" in {
    val httpResponse = mockHttpClient.get("https://www.test1.com/").futureValue

    httpResponse.isSuccess && httpResponse.isHTMLResource shouldBe true

    val links = HtmlUtils.linksOf(httpResponse.body, "https://www.test1.com/")

    links.size shouldBe 2
    links.foreach(HttpUtils.isValidHttp(_) shouldBe true)
    links.head shouldBe "https://www.test2.com/"
    links.tail.head shouldBe "https://www.test1.com/service1/"
    WordCount.wordsOf(HtmlUtils.toText(httpResponse.body)) shouldBe
      List("text", "response", "1")
  }

  "wordCountMonoid" should "add two WordCount instances correctly" in {
    WordCount(Map("a" -> 2, "b" -> 3, "c" -> 4)) |+| WordCount(Map("a" -> 2, "b" -> 3, "d" -> 5)) shouldBe
      WordCount(Map("a" -> 4, "b" -> 6, "c" -> 4, "d" -> 5))

    WordCount(Map("a" -> 2, "b" -> 3, "c" -> 4)) |+| wordCountMonoid.identity shouldBe
      WordCount(Map("a" -> 2, "b" -> 3, "c" -> 4))
  }

  "WordCounter should work correctly and crawl" should "return proper responses when depth is 0" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(0, sameDomainOnly = false))(WordCounter).futureValue.wordToCount shouldBe
      Map("text" -> 1, "response" -> 1, "1" -> 1)
  }

  it should "return proper responses when depth is 1" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(1, sameDomainOnly = false))(WordCounter).futureValue.wordToCount shouldBe
      Map("text" -> 2, "response" -> 2, "1" -> 1, "2" -> 1, "service1" -> 1)
  }

  it should "return proper responses when depth is 2" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(2, sameDomainOnly = false))(WordCounter).futureValue.wordToCount shouldBe
      Map("text" -> 3, "response" -> 3, "1" -> 1, "2" -> 1, "3" -> 1, "service1" -> 1, "service2" -> 1)
  }

  it should "return proper responses when depth is 3" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(3, sameDomainOnly = false))(WordCounter).futureValue.wordToCount shouldBe
      Map("text" -> 3, "response" -> 3, "1" -> 1, "2" -> 1, "3" -> 1, "service1" -> 1, "service2" -> 1, "service3" -> 1)
  }

  it should "return proper responses depth is a big number" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(1000, sameDomainOnly = false))(WordCounter).futureValue.wordToCount shouldBe
      Map("text" -> 3, "response" -> 3, "1" -> 1, "2" -> 1, "3" -> 1, "service1" -> 1, "service2" -> 1, "service3" -> 1)
  }

  it should "return proper responses when depth is a big number and same domain is true" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(1000))(WordCounter).futureValue.wordToCount shouldBe
      Map("text" -> 1, "response" -> 1, "1" -> 1, "service1" -> 1, "service2" -> 1, "service3" -> 1)
  }

  "FileOutput" should "generate files with proper http responses" in {
    val ex = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

    val tempDirName = "test-fileoutput-temporary-dir"
    val dir = new File(tempDirName)
    dir.mkdir

    val savedFiles =
      testSpidey.crawl("https://www.test1.com/",
        SpideyConfig(1000))(new FileOutput(tempDirName)(ex)).futureValue

    dir.listFiles.length shouldBe 4

    savedFiles.urlToPath.size shouldBe 4
    savedFiles.urlToPath.keySet.forall(url => url.startsWith("https://www.test1.com/")) shouldBe true

    def getFileContent(file: File) = {
      val src = Source.fromFile(file.getAbsolutePath)
      val fileContent = src.getLines.mkString("\n")
      src.close
      fileContent
    }

    val contentsThatShouldBeInFiles =
      List(MockHttpClient.test1Response,
        MockHttpClient.test1ResponseService1,
        MockHttpClient.test1ResponseService2,
        MockHttpClient.test1ResponseService3).map(FileOutput.responseToString)

    contentsThatShouldBeInFiles.forall {
      content => dir.listFiles.exists(getFileContent(_) == content)
    } shouldBe true

    dir.listFiles.foreach(_.delete)
    dir.delete
  }

  "BrokenLinkDetector" should "detect 0 urls with response code 404" in {
    testSpidey
      .crawl("https://www.test1.com/",
        SpideyConfig(2, sameDomainOnly = false))(BrokenLinkDetector).futureValue.isEmpty shouldBe true
  }

  it should "detect 1 url with response code 404" in {
    testSpidey
      .crawl("https://www.test1.com/",
        SpideyConfig(100))(BrokenLinkDetector).futureValue shouldBe
      Set("https://www.test1.com/service4/")
  }

  it should "detect 2 urls with response code 404" in {
    testSpidey
      .crawl("https://www.test1.com/",
        SpideyConfig(100, sameDomainOnly = false))(BrokenLinkDetector).futureValue shouldBe
      Set("https://www.test4.com/", "https://www.test1.com/service4/")
  }

  "When tolerateErrors is true, crawl" should "generate identity of the monoid" in {
    val future = testSpidey.crawl("https://www.INVALID.com/",
      SpideyConfig(3))(WordCounter)

    future.futureValue shouldBe WordCount(Map.empty)
  }

  "When tolerateErrors is false, crawl" should "generate failed future with IllegalArgumentException exception" in {
    val future = testSpidey.crawl("https://www.INVALID.com/",
      SpideyConfig(3, tolerateErrors = false))(WordCounter)

    assert(future.failed.futureValue.isInstanceOf[IllegalArgumentException])
  }

  it should "generate failed future with InvalidApplicationException exception" in {
    val future = testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(40, tolerateErrors = false, sameDomainOnly = false))(WordCounter)

    assert(future.failed.futureValue.isInstanceOf[InvalidApplicationException])
  }

  // tests for retriesOnError should be done with different http clients,
  // because of the mutable variable 'attemptForRetriesOnError' in mockHttpClient
  "retriesOnError" should "generate failed response when set to 0" in {
    val spidey = new Spidey(new MockHttpClient)

    val future1 = spidey.crawl("https://www.testRetriesOnError.com/",
      SpideyConfig(40, retriesOnError = 0, tolerateErrors = false))(WordCounter)

    assert(future1.failed.futureValue.isInstanceOf[InvalidApplicationException])
  }

  it should "generate failed response when set to 1" in {
    val spidey = new Spidey(new MockHttpClient)

    val future = spidey.crawl("https://www.testRetriesOnError.com/",
      SpideyConfig(40, retriesOnError = 1, tolerateErrors = false))(WordCounter)

    assert(future.failed.futureValue.isInstanceOf[InvalidApplicationException])
  }

  it should "generate successful response when set to 2" in {
    val spidey = new Spidey(new MockHttpClient)

    val future = spidey.crawl("https://www.testRetriesOnError.com/",
      SpideyConfig(40, retriesOnError = 3, tolerateErrors = false))(WordCounter)

    future.futureValue.wordToCount shouldBe
      Map("text" -> 1, "response" -> 1, "1" -> 1)
  }

  "LinkReferences" should "contain proper reference count" in {
    testSpidey
      .crawl("https://www.test1.com/",
        SpideyConfig(25))(LinkReferences).futureValue.linkToReferences shouldBe
      Map(
        "https://www.test1.com/service3/" -> 1,
        "https://www.test1.com/service4/" -> 1,
        "https://www.test2.com/" -> 1,
        "https://www.test1.com/service2/" -> 1,
        "https://www.test1.com/service1/" -> 1,
        "https://www.test1.com/" -> 3)
  }

  "WordOccurence" should "contain proper word occurence count" in {
    testSpidey
      .crawl("https://www.test5.com/",
        SpideyConfig(25))(WordOccurencerCounter).futureValue.linkWordOccurenceMap shouldBe
      Map(
        "https://www.test5.com/service3/" -> Map("fuck" -> 1, "dogs" -> 1, "and" -> 1, "cats" -> 1),
        "https://www.test5.com/service2/" -> Map("dog" -> 4, "cat" -> 2),
        "https://www.test5.com/service1/" -> Map("dog" -> 2, "cat" -> 1),
        "https://www.test5.com/" -> Map("dog" -> 3, "cat" -> 2))
  }
}

object Test extends App {
  val httpClient = new AsyncHttpClient
  val spidey = new Spidey(httpClient)

  def getFutureResultBlocking[R](f: Future[R]) = Await.result(f, Duration.Inf)

  println(getFutureResultBlocking(spidey
    .crawl("https://en.wikipedia.org/wiki/Adolf_Hitler",
      SpideyConfig(0))(WordCounter)).wordToCount)
}

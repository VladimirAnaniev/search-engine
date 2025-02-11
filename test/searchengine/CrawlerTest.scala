package searchengine

import javax.management.InvalidApplicationException
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import searchengine.html.HtmlUtils
import searchengine.http.HttpUtils
import searchengine.math.Monoid._
import searchengine.math.Monoid.ops._
import searchengine.processors._

import scala.concurrent.ExecutionContext.Implicits.global

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
    WordCount.wordsOf(HtmlUtils.toText(httpResponse.body))  should contain theSameElementsAs
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
      SpideyConfig(0, sameDomainOnly = false))(WordCounter).futureValue.wordToCount  should contain theSameElementsAs
      Map("text" -> 1, "response" -> 1, "1" -> 1)
  }

  it should "return proper responses when depth is 1" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(1, sameDomainOnly = false))(WordCounter).futureValue.wordToCount  should contain theSameElementsAs
      Map("text" -> 2, "response" -> 2, "1" -> 1, "2" -> 1, "service1" -> 1)
  }

  it should "return proper responses when depth is 2" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(2, sameDomainOnly = false))(WordCounter).futureValue.wordToCount  should contain theSameElementsAs
      Map("text" -> 3, "response" -> 3, "1" -> 1, "2" -> 1, "3" -> 1, "service1" -> 1, "service2" -> 1)
  }

  it should "return proper responses when depth is 3" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(3, sameDomainOnly = false))(WordCounter).futureValue.wordToCount  should contain theSameElementsAs
      Map("text" -> 3, "response" -> 3, "1" -> 1, "2" -> 1, "3" -> 1, "service1" -> 1, "service2" -> 1, "service3" -> 1)
  }

  it should "return proper responses depth is a big number" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(1000, sameDomainOnly = false))(WordCounter).futureValue.wordToCount  should contain theSameElementsAs
      Map("text" -> 3, "response" -> 3, "1" -> 1, "2" -> 1, "3" -> 1, "service1" -> 1, "service2" -> 1, "service3" -> 1)
  }

  it should "return proper responses when depth is a big number and same domain is true" in {
    testSpidey.crawl("https://www.test1.com/",
      SpideyConfig(1000))(WordCounter).futureValue.wordToCount  should contain theSameElementsAs
      Map("text" -> 1, "response" -> 1, "1" -> 1, "service1" -> 1, "service2" -> 1, "service3" -> 1)
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

    future.futureValue.wordToCount  should contain theSameElementsAs
      Map("text" -> 1, "response" -> 1, "1" -> 1)
  }

  "LinkReferences" should "contain proper reference count" in {
    testSpidey
      .crawl("https://www.test1.com/",
        SpideyConfig(25))(LinkReferences).futureValue.linkToReferences  should contain theSameElementsAs
      Map(
        ("https://www.test1.com/service3/", "https://www.test1.com/service4/") -> 1,
        ("https://www.test1.com/service2/", "https://www.test1.com/service3/") -> 1,
        ("https://www.test1.com/", "https://www.test2.com/") -> 1,
        ("https://www.test1.com/service1/", "https://www.test1.com/") -> 1,
        ("https://www.test1.com/service3/", "https://www.test1.com/") -> 1,
        ("https://www.test1.com/service2/", "https://www.test1.com/") -> 1,
        ("https://www.test1.com/", "https://www.test1.com/service1/") -> 1,
        ("https://www.test1.com/service1/", "https://www.test1.com/service2/") -> 1)
  }

  "WordOccurence" should "contain proper word occurence count" in {
    testSpidey
      .crawl("https://www.test5.com/",
        SpideyConfig(25))(WordOccurrenceCounter).futureValue.linkWordOccurrenceMap should contain theSameElementsAs
        Map(("https://www.test5.com/","cat") -> 2,
          ("https://www.test5.com/service3/","fuck") -> 1,
          ("https://www.test5.com/service2/","cat") -> 2,
          ("https://www.test5.com/service3/","dogs") -> 1,
          ("https://www.test5.com/service1/","cat") -> 1,
          ("https://www.test5.com/service3/","and") -> 1,
          ("https://www.test5.com/service3/","cats") -> 1,
          ("https://www.test5.com/service1/","dog") -> 2,
          ("https://www.test5.com/service2/","dog") -> 4,
          ("https://www.test5.com/","dog") -> 3)
  }
}

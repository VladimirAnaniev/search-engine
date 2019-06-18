package homework3

import homework3.html.HtmlUtils
import homework3.http.HttpUtils
import homework3.processors.{WordCount, WordCounter}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class Test extends FlatSpec with Matchers {
  val mockHttpClient = new MockHttpClient
  val testSpidey = new Spidey(mockHttpClient)

  "response from MockHttpClient" should "be html resource containing valid http link" in {
    val httpResponse = Await.result(mockHttpClient.get("https://www.test1.com/"), Duration.Inf)

    httpResponse.isSuccess shouldBe true

    httpResponse.isHTMLResource shouldBe true

    val links = HtmlUtils.linksOf(httpResponse.body, "https://www.test1.com/")

    links.size shouldBe 1
    links.foreach(HttpUtils.isValidHttp(_) shouldBe true)
    links.head shouldBe "https://www.test2.com/"
    WordCount.wordsOf(httpResponse.body) shouldBe
      List("a", "href", "https", "www", "test2", "com", "Visit", "our", "HTML", "tutorial", "a")
  }

  "crawl" should "go to only one link when depth is 0" in {
    // testSpidey.crawl("https://www.test1.com/", SpideyConfig(0))(WordCounter)
  }


}

object Test extends App {
  val mockHttpClient = new MockHttpClient

  import homework3.processors.WordCount
  println(WordCount.wordsOf(Await.result(mockHttpClient.get("https://www.test1.com/"), Duration.Inf).body))

}

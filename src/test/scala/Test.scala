package homework3

import homework3.html.HtmlUtils
import homework3.http.HttpUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class Test extends FlatSpec with Matchers {
  val mockHttpClient = new MockHttpClient

  "response from MockHttpClient" should "be html resource containing valid http link" in {
    val httpResponse = Await.result(mockHttpClient.get("https://www.test1.com/"), Duration.Inf)

    httpResponse.isSuccess shouldBe true

    httpResponse.isHTMLResource shouldBe true

    val links = HtmlUtils.linksOf(httpResponse.body, "https://www.test1.com/")

    links.size shouldBe 1
    links.foreach(HttpUtils.isValidHttp(_) shouldBe true)
    links.head shouldBe "https://www.test2.com/"
  }

}

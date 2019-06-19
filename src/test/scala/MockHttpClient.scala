package homework3

import homework3.http.{HttpClient, HttpResponse}

import scala.concurrent.{Future, Promise}
import scala.util.Try

class MockHttpResponseFactory {
  def createResponse(responseBody: String) = new HttpResponse {
    def status: Int = 200

    def headers: Map[String, String] = Map("content-type" -> "text/html")

    override def body = responseBody

    def bodyAsBytes: Array[Byte] = Array(0)
  }
}

class MockHttpClient extends HttpClient {
  def get(url: String): Future[HttpResponse] = url match {
    case "https://www.test1.com/" => Promise[HttpResponse].complete(Try(MockHttpClient.testResponse2)).future
    case "https://www.test2.com/" => Promise[HttpResponse].complete(Try(MockHttpClient.testResponse3)).future
    case "https://www.test3.com/" => Promise[HttpResponse].complete(Try(MockHttpClient.testResponse1)).future
    case _ => throw new IllegalArgumentException("Wrong testing url!")
  }
}

object MockHttpClient {
  val mockHttpResponseFactory = new MockHttpResponseFactory

  val textResponse1 = "text-response-1"
  val linksResponse1 = List("https://www.test2.com/")

  val textResponse2 = "text-response-2"
  val linksResponse2 = List("https://www.test3.com/")

  val textResponse3 = "text-response-3"
  val linksResponse3 = List("https://www.test1.com/")

  val testResponse1: HttpResponse =
    mockHttpResponseFactory.createResponse(s""""<a href="https://www.test1.com/">$textResponse1</a>"""")

  val testResponse2: HttpResponse =
    mockHttpResponseFactory.createResponse(s""""<a href="https://www.test2.com/">$textResponse2</a>"""")

  val testResponse3: HttpResponse =
    mockHttpResponseFactory.createResponse(s""""<a href="https://www.test3.com/">$textResponse3</a>"""")
}



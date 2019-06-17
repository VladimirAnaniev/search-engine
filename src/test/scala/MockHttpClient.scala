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

  val mockHttpResponseFactory = new MockHttpResponseFactory

  val testResponse1 =
    mockHttpResponseFactory.createResponse("<a href=\"https://www.test1.com/\">Visit our HTML tutorial</a>")

  val testResponse2 =
    mockHttpResponseFactory.createResponse("<a href=\"https://www.test2.com/\">Visit our HTML tutorial</a>")

  val testResponse3 =
    mockHttpResponseFactory.createResponse("<a href=\"https://www.test3.com/\">Visit our HTML tutorial</a>")

  def get(url: String): Future[HttpResponse] = url match {
    case "https://www.test1.com/" => Promise[HttpResponse].complete(Try(testResponse2)).future
    case "https://www.test2.com/" => Promise[HttpResponse].complete(Try(testResponse3)).future
    case "https://www.test3.com/" => Promise[HttpResponse].complete(Try(testResponse1)).future
    case _ => throw new IllegalArgumentException("Wrong testing url!")
  }
}



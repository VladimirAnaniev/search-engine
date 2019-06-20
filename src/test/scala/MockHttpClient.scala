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

  def createEmptyResponse = createResponse("")
}

class MockHttpClient extends HttpClient {
  def get(url: String): Future[HttpResponse] = url match {
    case "https://www.test1.com/" => MockHttpClient.test1Response
    case "https://www.test1.com/service1/" => MockHttpClient.test1ResponseService1
    case "https://www.test1.com/service2/" => MockHttpClient.test1ResponseService2
    case "https://www.test1.com/service3/" => MockHttpClient.test1ResponseService3
    case "https://www.test2.com/" => MockHttpClient.test2Response
    case "https://www.test3.com/" => MockHttpClient.test3Response
    case wrongURL => throw new IllegalArgumentException("Wrong testing url - " + wrongURL)
  }
}

object MockHttpClient {
  val mockHttpResponseFactory = new MockHttpResponseFactory

  val textResponse1 = "text response 1"
  val textResponse2 = "text response 2"
  val textResponse3 = "text response 3"

  val emptyResponse = mockHttpResponseFactory.createEmptyResponse

  def test1Response = Promise[HttpResponse].complete(Try(
    mockHttpResponseFactory.createResponse(
      s""""<a href="https://www.test2.com/">$textResponse1</a>
         |<a href="https://www.test1.com/service1/"></a>"""".stripMargin))).future

  def test1ResponseService1 = Promise[HttpResponse].complete(Try(
    MockHttpClient.mockHttpResponseFactory
      .createResponse(s""""<a href="https://www.test1.com/service2/">service1</a>"""))).future

  def test1ResponseService2 = Promise[HttpResponse].complete(Try(
    MockHttpClient.mockHttpResponseFactory
      .createResponse(s""""<a href="https://www.test1.com/service3/">service2</a>"""))).future

  def test1ResponseService3 = Promise[HttpResponse].complete(Try(
    MockHttpClient.mockHttpResponseFactory
      .createResponse("service3"))).future

  def test2Response = Promise[HttpResponse].complete(Try(
    mockHttpResponseFactory.createResponse(
      s""""<a href="https://www.test3.com/">$textResponse2</a>"""))).future

  def test3Response = Promise[HttpResponse].complete(Try(
    mockHttpResponseFactory.createResponse(
      s""""<a href="https://www.test1.com/">$textResponse3</a>"""))).future
}



package homework3

import homework3.http.{HttpClient, HttpResponse}

import scala.concurrent.{Future, Promise}
import scala.util.Try

class MockHttpResponseFactory {
  def createHttpResponse(responseBody: String, statusCode: Int = 200) = new HttpResponse {
    def status: Int = statusCode

    def headers: Map[String, String] = Map("content-type" -> "text/html")

    override def body = responseBody

    def bodyAsBytes: Array[Byte] = Array(0)
  }
}

class MockHttpClient extends HttpClient {
  def get(url: String): Future[HttpResponse] = url match {
    case "https://www.test1.com/" => Promise[HttpResponse].complete(Try(MockHttpClient.test1Response)).future
    case "https://www.test1.com/service1/" => Promise[HttpResponse].complete(Try(MockHttpClient.test1ResponseService1)).future
    case "https://www.test1.com/service2/" => Promise[HttpResponse].complete(Try(MockHttpClient.test1ResponseService2)).future
    case "https://www.test1.com/service3/" => Promise[HttpResponse].complete(Try(MockHttpClient.test1ResponseService3)).future
    case "https://www.test1.com/service4/" => Promise[HttpResponse].complete(Try(MockHttpClient.test1ResponseService4)).future
    case "https://www.test2.com/" => Promise[HttpResponse].complete(Try(MockHttpClient.test2Response)).future
    case "https://www.test3.com/" => Promise[HttpResponse].complete(Try(MockHttpClient.test3Response)).future
    case "https://www.test4.com/" => Promise[HttpResponse].complete(Try(MockHttpClient.test4Response)).future
    case wrongURL => throw new IllegalArgumentException("Wrong testing url - " + wrongURL)
  }
}

object MockHttpClient {
  val mockHttpResponseFactory = new MockHttpResponseFactory

  val textResponse1 = "text response 1"
  val textResponse2 = "text response 2"
  val textResponse3 = "text response 3"

  def test1Response =
    mockHttpResponseFactory.createHttpResponse(
      s""""<a href="https://www.test2.com/">$textResponse1</a>
         |<a href="https://www.test1.com/service1/"></a>"""".stripMargin)

  def test1ResponseService1 =
    mockHttpResponseFactory.createHttpResponse(s""""<a href="https://www.test1.com/service2/">service1</a>""")

  def test1ResponseService2 =
    mockHttpResponseFactory.createHttpResponse(s""""<a href="https://www.test1.com/service3/">service2</a>""")

  def test1ResponseService3 =
    mockHttpResponseFactory.createHttpResponse(s""""<a href="https://www.test1.com/service4/">service3</a>""")

  def test1ResponseService4 =
    mockHttpResponseFactory.createHttpResponse("service4notfound", 404)

  def test2Response =
    mockHttpResponseFactory.createHttpResponse(
      s""""<a href="https://www.test3.com/">$textResponse2</a>""")

  def test3Response =
    mockHttpResponseFactory.createHttpResponse(
      s""""<a href="https://www.test1.com/">$textResponse3</a>
         |<a href="https://www.test4.com/"></a>
     """.stripMargin)

  def test4Response =
    mockHttpResponseFactory.createHttpResponse("test4notfound", 404)
}



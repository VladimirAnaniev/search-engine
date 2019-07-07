package searchengine

import javax.management.InvalidApplicationException
import searchengine.http.{HttpClient, HttpResponse}

import scala.concurrent.Future

class MockHttpResponseFactory {
  def createHttpResponse(responseBody: String, statusCode: Int = 200) = new HttpResponse {
    def status: Int = statusCode

    def headers: Map[String, String] = Map("content-type" -> "text/html")

    override def body = responseBody

    def bodyAsBytes: Array[Byte] = Array(0)
  }
}

class MockHttpClient extends HttpClient {

  var attemptForRetriesOnError = 0

  def get(url: String): Future[HttpResponse] = url match {
    case "https://www.test1.com/" => Future.successful(MockHttpClient.test1Response)
    case "https://www.test1.com/service1/" => Future.successful(MockHttpClient.test1ResponseService1)
    case "https://www.test1.com/service2/" => Future.successful(MockHttpClient.test1ResponseService2)
    case "https://www.test1.com/service3/" => Future.successful(MockHttpClient.test1ResponseService3)
    case "https://www.test1.com/service4/" => Future.successful(MockHttpClient.test1ResponseService4)
    case "https://www.test2.com/" => Future.successful(MockHttpClient.test2Response)
    case "https://www.test2.com/service1/" => Future.successful(MockHttpClient.test2ResponseService1)
    case "https://www.test2.com/service2/" => Future.failed(new InvalidApplicationException("No such service!"))
    case "https://www.test3.com/" => Future.successful(MockHttpClient.test3Response)
    case "https://www.test4.com/" => Future.successful(MockHttpClient.test4Response)
    case "https://www.test5.com/" => Future.successful(MockHttpClient.test5Response)
    case "https://www.test5.com/service1/" => Future.successful(MockHttpClient.test5ResponseService1)
    case "https://www.test5.com/service2/" => Future.successful(MockHttpClient.test5ResponseService2)
    case "https://www.test5.com/service3/" => Future.successful(MockHttpClient.test5ResponseService3)
    case "https://www.testRetriesOnError.com/" => {
      attemptForRetriesOnError += 1
      if (attemptForRetriesOnError % 3 == 0) {
        Future.successful(MockHttpClient.test1Response)
      } else {
        Future.failed(new InvalidApplicationException("Service not available yet. Try again."))
      }
    }
    case wrongURL => Future.failed(new IllegalArgumentException("Wrong testing url - " + wrongURL))
  }
}

object MockHttpClient {
  val mockHttpResponseFactory = new MockHttpResponseFactory

  val textResponse1 = "text response 1"
  val textResponse2 = "text response 2"
  val textResponse3 = "text response 3"

  val test1Response =
    mockHttpResponseFactory.createHttpResponse(
      s""""<a href="https://www.test2.com/">$textResponse1</a>
         |<a href="https://www.test1.com/service1/"></a>"""".stripMargin)

  val test1ResponseService1 =
    mockHttpResponseFactory.createHttpResponse(
      s""""<a href="https://www.test1.com/service2/">service1</a>
         |<a href="https://www.test1.com/"></a>"""".stripMargin)

  val test1ResponseService2 =
    mockHttpResponseFactory.createHttpResponse(
      s""""<a href="https://www.test1.com/service3/">service2</a>
         |<a href="https://www.test1.com/"></a>""".stripMargin)

  val test1ResponseService3 =
    mockHttpResponseFactory.createHttpResponse(
      s""""<a href="https://www.test1.com/service4/">service3</a>
         |<a href="https://www.test1.com/"></a>""".stripMargin)

  val test1ResponseService4 =
    mockHttpResponseFactory.createHttpResponse(
      """<a href="https://www.test1.com/">service4notfound</a>""", 404)

  val test2Response =
    mockHttpResponseFactory.createHttpResponse(
      s""""<a href="https://www.test3.com/">$textResponse2</a>
         |<a href="https://www.test2.com/service1/"></a>"""".stripMargin)

  val test2ResponseService1 =
    mockHttpResponseFactory.createHttpResponse(
      s""""<a href="https://www.test2.com/service2/"></a>""")

  val test3Response =
    mockHttpResponseFactory.createHttpResponse(
      s""""<a href="https://www.test1.com/">$textResponse3</a>
         |<a href="https://www.test4.com/"></a>
     """.stripMargin)

  val test4Response =
    mockHttpResponseFactory.createHttpResponse("test4notfound", 404)

  val test5Response =
    mockHttpResponseFactory.createHttpResponse(
      """<a href="https://www.test5.com/service1/">dog dog dog cat cat</a>""")

  val test5ResponseService1 =
    mockHttpResponseFactory.createHttpResponse(
      """<a href="https://www.test5.com/service2/">dog dog cat</a>""")

  val test5ResponseService2 =
    mockHttpResponseFactory.createHttpResponse(
      """<a href="https://www.test5.com/service3/">dog dog dog dog cat cat</a>""")

  val test5ResponseService3 =
    mockHttpResponseFactory.createHttpResponse(
      """fuck dogs and cats""")
}



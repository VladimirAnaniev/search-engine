package homework3

import java.util.concurrent.Executors

import homework3.http.AsyncHttpClient
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class Test extends FlatSpec with Matchers {

  val httpClient = new AsyncHttpClient
  val threadPool = Executors.newFixedThreadPool(32)
  implicit val ec = ExecutionContext.fromExecutor(threadPool)

  "response from HTTP GET request from https://www.google.com/" should "be html resource" in {
    val futureResponse = httpClient.get("https://www.google.com/")

    futureResponse onComplete {
      case Success(response) => response.isHTMLResource shouldBe true
      case Failure(_) => fail("Failed HTTP GET request!")
    }

    Await.ready(futureResponse, Duration.Inf)
  }
}

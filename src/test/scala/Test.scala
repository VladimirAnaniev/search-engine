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
    Await.result(httpClient.get("https://www.google.com/").map(_.isHTMLResource), Duration.Inf) shouldBe true
  }
}

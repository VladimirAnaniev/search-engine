package homework3

import java.util.concurrent.ForkJoinPool

import homework3.html.HtmlUtils
import homework3.http.AsyncHttpClient
import homework3.processors.WordCount

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object SpideyApp {
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool)
  val blockingExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(4))

  val httpClient = new AsyncHttpClient
  val spidey = new Spidey(httpClient)

  def printUsage: Unit = {
    println(
      """
        |Usage:
        |
        |SpideyApp <url> <max-depth> <processor> [processor-config]
        |
        |Possible processors and their config are:
        |
        |file-output <target-dir>
        |word-counter
        |broken-link-detector
      """.stripMargin)
  }

  def main(args: Array[String]): Unit = {

    //    TODO: Figure out how to call crawl
    //    spidey.crawl("https://www.google.com/", SpideyConfig(maxDepth = 0))

    if (args.isEmpty /* or arguments are invalid */ ) printUsage
    else {
      // process arguments

      // run Spidey

      Await.result(???, Duration.Inf)

      // output result
    }

    httpClient.shutdown()
  }
}

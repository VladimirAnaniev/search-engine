package homework3

import homework3.html.HtmlUtils
import homework3.http._
import homework3.math.Monoid

import scala.collection.immutable.HashSet
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

case class SpideyConfig(maxDepth: Int,
                        sameDomainOnly: Boolean = true,
                        tolerateErrors: Boolean = true,
                        retriesOnError: Int = 0)

class Spidey(httpClient: HttpClient)(implicit ex: ExecutionContext) {
  def crawl[O: Monoid](url: String, config: SpideyConfig)
                      (processor: Processor[O]): Future[O] = Future {

    if (config.maxDepth < 0) {
      throw new IllegalArgumentException("maxDepth cannot be less than zero!")
    }

    def crawlRecHelper(visited: HashSet[String], toVisit: List[String], curResult: O, curDepth: Int): O = {
      if (toVisit.isEmpty) {
        curResult
      }
      else {
        // do get requests on all URLs and wait for responses
        val urlAndHttpResponseTuple =
          toVisit
            .map(url => (url, httpClient.get(url)))
            .map(tuple => (tuple._1, Await.result(tuple._2, Duration.Inf)))

        // process all the responses and wait for result
        val resultFromProcessing = urlAndHttpResponseTuple.map(tuple => processor(tuple._1, tuple._2))
          .map(Await.result(_, Duration.Inf))
          .foldLeft(implicitly[Monoid[O]].identity)(implicitly[Monoid[O]].op(_, _))

        // TODO SG: As soon as response is received from a given url, start the processor.
        // (Currently we wait for all responses to finish and then begin processing them)

        if (curDepth > 0) {
          crawlRecHelper(
            visited ++ toVisit,
            urlAndHttpResponseTuple.flatMap(pair => {
              if (pair._2.isHTMLResource) {
                HtmlUtils.linksOf(pair._2.body, pair._1).filter(!visited(_))
              } else {
                List.empty
              }
            }),
            Monoid[O].op(resultFromProcessing, curResult),
            curDepth - 1)
        }
        else {
          crawlRecHelper(
            HashSet.empty,
            List.empty,
            Monoid[O].op(resultFromProcessing, curResult),
            curDepth - 1
          )
        }
      }
    }

    crawlRecHelper(HashSet.empty, List(url), Monoid[O].identity, config.maxDepth)
  }
}

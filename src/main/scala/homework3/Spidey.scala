package homework3

import homework3.html.HtmlUtils
import homework3.http._
import homework3.math.Monoid

import scala.collection.mutable
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

    var visited: mutable.HashSet[String] = mutable.HashSet.empty

    var toVisit: mutable.Queue[String] = mutable.Queue.empty
    toVisit.enqueue(url)

    var results: mutable.MutableList[O] = mutable.MutableList.empty

    var curDepth = config.maxDepth

    while (toVisit.nonEmpty) {
      curDepth -= 1
      visited ++= toVisit

      val urlAndHttpResponseTuple = toVisit.map(url => (url, httpClient.get(url))).map(tuple => (tuple._1, Await.result(tuple._2, Duration.Inf)))

      toVisit.clear

      // only add new links to visit if depth is still >= 0
      if (curDepth >= 0) {
        urlAndHttpResponseTuple.foreach(tuple => {
          if (tuple._2.isHTMLResource) {
            toVisit ++= HtmlUtils.linksOf(tuple._1, tuple._2.body).filter(!visited(_))
          }
        })
      }

      results +=
        urlAndHttpResponseTuple.map(tuple => processor(tuple._1, tuple._2))
          .map(Await.result(_, Duration.Inf))
          .foldLeft(implicitly[Monoid[O]].identity)(implicitly[Monoid[O]].op(_, _))
    }

    results.foldLeft(implicitly[Monoid[O]].identity)(implicitly[Monoid[O]].op(_, _))
  }
}

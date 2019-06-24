package homework3

import homework3.html.HtmlUtils
import homework3.http._
import homework3.math.Monoid

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

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

    @tailrec
    def crawlRecHelper(visited: HashSet[String], toVisit: List[String], curResult: O, curDepth: Int): O = {
      import homework3.math.Monoid.ops._

      val urlToResponseMap: mutable.Map[String, HttpResponse] = mutable.Map.empty

      def processUrl(url: String): Future[O] = (httpClient.get(url) andThen {
        case Success(response) => urlToResponseMap += url -> response
      }).flatMap(response => processor(url, response))

      val processedUrls = toVisit.map(processUrl)

      processedUrls.foreach(Await.ready(_, Duration.Inf))

      val result = processedUrls
        .map(_.value match {
          case Some(Success(r)) => r
          case Some(Failure(t)) => if (config.tolerateErrors) Monoid[O].identity else throw t
        })
        .foldLeft(Monoid[O].identity)(_ |+| _)

      if (curDepth > 0) {
        def shouldBeVisited(link: String) = if (config.sameDomainOnly) {
          !visited(link) && HttpUtils.sameDomain(url, link)
        } else {
          !visited(link)
        }

        val newToVisit = urlToResponseMap.flatMap { case (url, response) =>
          if (response.isHTMLResource) {
            HtmlUtils.linksOf(response.body, url).distinct.filter(shouldBeVisited)
          } else {
            List.empty
          }
        }.toList

        crawlRecHelper(
          visited ++ toVisit,
          newToVisit,
          result |+| curResult,
          curDepth - 1)
      }
      else {
        result |+| curResult
      }
    }

    crawlRecHelper(HashSet.empty, List(url), Monoid[O].identity, config.maxDepth)
  }
}

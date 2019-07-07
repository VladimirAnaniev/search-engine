package searchengine

import searchengine.html.HtmlUtils
import searchengine.http._
import searchengine.math.Monoid

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Success

case class SpideyConfig(maxDepth: Int,
                        sameDomainOnly: Boolean = true,
                        tolerateErrors: Boolean = true,
                        retriesOnError: Int = 0)

class Spidey(httpClient: HttpClient)(implicit ex: ExecutionContext) {
  def crawl[O: Monoid](url: String, config: SpideyConfig)
                      (processor: Processor[O]): Future[O] = Future {
    validateConfig(config)

    @tailrec
    def crawlRecHelper(visited: HashSet[String], toVisit: List[String], curResult: O, curDepth: Int): O = {
      import searchengine.math.Monoid.ops._

      val urlToResponseMap: mutable.Map[String, HttpResponse] = mutable.Map.empty

      def processUrl(url: String): Future[O] = doGetRequest(url, config.retriesOnError)
        .andThen {
          case Success(response) => urlToResponseMap += response.url -> response
        }
        .flatMap { response => processor(response.url, response) }
        .recover {
          case t => if (config.tolerateErrors) Monoid[O].identity else throw t
        }

      val result = toVisit
        .map(processUrl)
        .map(Await.result(_, Duration.Inf))
        .foldLeft(Monoid[O].identity)(_ |+| _)

      if (curDepth > 0) {
        def shouldBeVisited(link: String) = if (config.sameDomainOnly) {
          !visited(link) && HttpUtils.sameDomain(url, link)
        } else {
          !visited(link)
        }

        val newToVisit = urlToResponseMap.flatMap { case (url, response) =>
          if (response.isHTMLResource) {
            HtmlUtils.linksOf(response.body, url).distinct.filter(HttpUtils.isValidHttp).filter(shouldBeVisited)
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

  def doGetRequest(url: String, retriesOnError: Int = 0): Future[HttpResponse] = {
    if (retriesOnError == 0) {
      httpClient.get(url)
    } else {
      httpClient.get(url).recoverWith[HttpResponse] { case _ => doGetRequest(url, retriesOnError - 1) }
    }
  }

  def validateConfig(config: SpideyConfig): Unit = {
    if (config.maxDepth < 0) {
      throw new IllegalArgumentException("maxDepth cannot be less than zero!")
    } else if (config.retriesOnError < 0) {
      throw new IllegalArgumentException("retriesOnError cannot be less than zero!")
    }
  }
}

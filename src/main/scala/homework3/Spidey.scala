package homework3

import homework3.html.HtmlUtils
import homework3.http._
import homework3.math.Monoid

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
    if (config.maxDepth < 0) {
      throw new IllegalArgumentException("maxDepth cannot be less than zero!")
    }

    import homework3.math.Monoid.ops._

    @tailrec
    def crawlRecHelper(visited: HashSet[String], toVisit: List[String], curResult: O, curDepth: Int): O = {
      val urlToResponseMap: mutable.Map[String, HttpResponse] = mutable.Map.empty

      val result = toVisit.map(url =>
        (httpClient.get(url) andThen {
          case Success(httpResponse) => urlToResponseMap += url -> httpResponse
        }).flatMap(processor(url, _)))
        .map(Await.result(_, Duration.Inf))
        .foldLeft(Monoid[O].identity)(_ |+| _)

      if (curDepth > 0) {
        crawlRecHelper(
          visited ++ toVisit,
          urlToResponseMap.toList.flatMap(keyValuePair => {
            if (keyValuePair._2.isHTMLResource) {
              HtmlUtils.linksOf(keyValuePair._2.body, keyValuePair._1).filter(link => {
                println(keyValuePair._1 + " -> " + link)
                if (config.sameDomainOnly) {
                  !visited(link) && HttpUtils.sameDomain(url, link)
                } else {
                  !visited(link)
                }
              })
            } else {
              List.empty
            }
          }),
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

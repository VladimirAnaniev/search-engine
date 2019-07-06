package searchengine.processors

import searchengine.Processor
import searchengine.html.HtmlUtils
import searchengine.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class LinkReferencesMap(linkToReferences: Map[String, Int])

object LinkReferences extends Processor[LinkReferencesMap] {
  def apply(url: String, response: HttpResponse): Future[LinkReferencesMap] = Future {
    if (response.isSuccess && (response.isHTMLResource || response.isPlainTextResource)) {
      val references = HtmlUtils.linksOf(response.body, url).foldLeft(Map.empty[String, Int]) {
        (count, link) => count + (link -> (count.getOrElse(link, 0) + 1))
      }

      LinkReferencesMap(references)
    }
    else LinkReferencesMap(Map.empty)
  }
}

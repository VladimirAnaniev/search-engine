package searchengine.processors

import searchengine.Processor
import searchengine.html.HtmlUtils
import searchengine.http.HttpResponse
import searchengine.math.Monoid

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class LinkReferencesMap(linkToReferences: Map[(String, String), Int])

object LinkReferences extends Processor[LinkReferencesMap] {
  def apply(url: String, response: HttpResponse): Future[LinkReferencesMap] = Future {
    if (response.isSuccess && (response.isHTMLResource || response.isPlainTextResource)) {

      val references = HtmlUtils.linksOf(response.body, url).foldLeft(Map.empty[(String, String), Int]) {
        case (acc, ref) => acc + ((url, ref) -> (acc.getOrElse((url, ref), 0) + 1))
      }

      LinkReferencesMap(references)
    }
    else Monoid[LinkReferencesMap].identity
  }
}

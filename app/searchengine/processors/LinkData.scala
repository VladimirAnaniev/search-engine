package searchengine.processors

import searchengine.Processor
import searchengine.http.HttpResponse
import searchengine.math.Monoid

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class LinkData(wordOccurrence: WordOccurrence, linkReferences: LinkReferencesMap)

object LinkDataProcessor extends Processor[LinkData] {
  def apply(url: String, response: HttpResponse): Future[LinkData] =
    if (response.isSuccess && (response.isHTMLResource || response.isPlainTextResource))
      for {
        count <- WordOccurrenceCounter(url, response)
        linkReferences <- LinkReferences(url, response)
      } yield LinkData(count, linkReferences)
    else
      Future.successful(Monoid[LinkData].identity)
}
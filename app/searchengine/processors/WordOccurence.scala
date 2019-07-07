package searchengine.processors

import searchengine.Processor
import searchengine.http.HttpResponse
import searchengine.math.Monoid
import searchengine.processors.WordOccurrence.{Link, OccurrenceCount, Word}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class WordOccurrence(linkWordOccurrenceMap: Map[(Link, Word), OccurrenceCount])

object WordOccurrence {
  type Link = String
  type OccurrenceCount = Int
  type Word = String
}

object WordOccurrenceCounter extends Processor[WordOccurrence] {
  def apply(url: String, response: HttpResponse): Future[WordOccurrence] =
    if (response.isSuccess && (response.isHTMLResource || response.isPlainTextResource))
      WordCounter(url, response).map(_.wordToCount.map {
        case (word, count) => (url, word) -> count
      }).map(WordOccurrence(_))
    else
      Future.successful(Monoid[WordOccurrence].identity)
}

package searchengine.processors

import searchengine.Processor
import searchengine.http.HttpResponse
import searchengine.processors.WordOccurence.{Link, OccurrenceCount, Word}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class WordOccurence(linkWordOccurenceMap: Map[Link, Map[Word, OccurrenceCount]])

object WordOccurence {
  type Link = String
  type OccurrenceCount = Int
  type Word = String

  def wordsOf(text: String): List[String] = text.split("\\W+").toList.filter(_.nonEmpty)
}

object WordOccurencerCounter extends Processor[WordOccurence] {
  def apply(url: String, response: HttpResponse): Future[WordOccurence] =
    if (response.isSuccess && (response.isHTMLResource || response.isPlainTextResource))
      WordCounter(url, response).map(_.wordToCount).map(count => WordOccurence(Map(url -> count)))
    else
      Future.successful(WordOccurence(Map.empty))
}

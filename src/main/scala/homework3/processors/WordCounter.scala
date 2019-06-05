package homework3.processors

import homework3.Processor
import homework3.http.HttpResponse

import scala.concurrent.Future

case class WordCount(wordToCount: Map[String, Int])

object WordCount {
  def wordsOf(text: String): List[String] = text.split("\\W+").toList.filter(_.nonEmpty)
}

object WordCounter extends Processor[WordCount] {
  def apply(url: String, response: HttpResponse): Future[WordCount] = ???
}

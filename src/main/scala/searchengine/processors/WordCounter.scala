package searchengine.processors

import searchengine.Processor
import searchengine.html.HtmlUtils
import searchengine.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class WordCount(wordToCount: Map[String, Int])

object WordCount {
  def wordsOf(text: String): List[String] = text.split("\\W+").toList.filter(_.nonEmpty)
}

object WordCounter extends Processor[WordCount] {
  def apply(url: String, response: HttpResponse): Future[WordCount] = Future {
    if (response.isSuccess && (response.isHTMLResource || response.isPlainTextResource)) {
      WordCount(
        WordCount.wordsOf(HtmlUtils.toText(response.body)).foldLeft(Map.empty[String, Int]) {
          (count, word) => count + (word -> (count.getOrElse(word, 0) + 1))
        })
    }
    else {
      WordCount(Map.empty)
    }
  }
}

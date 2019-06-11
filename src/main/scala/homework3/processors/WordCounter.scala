package homework3.processors

import java.util.concurrent.Executors

import homework3.Processor
import homework3.html.HtmlUtils
import homework3.http.HttpResponse

import scala.concurrent.{ExecutionContext, Future}

case class WordCount(wordToCount: Map[String, Int])

object WordCount {
  def wordsOf(text: String): List[String] = text.split("\\W+").toList.filter(_.nonEmpty)
}

object WordCounter extends Processor[WordCount] {
  val threadPool = Executors.newFixedThreadPool(32)
  implicit val ec = ExecutionContext.fromExecutor(threadPool)

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

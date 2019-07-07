package searchengine.processors

import searchengine.Processor
import searchengine.http.HttpResponse
import searchengine.math.Monoid

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BrokenLinkDetector extends Processor[Set[String]] {
  def apply(url: String, response: HttpResponse): Future[Set[String]] = Future {
    response.status match {
      case 404 => Set(url)
      case _ => Monoid[Set[String]].identity
    }
  }
}

package homework3.processors

import homework3.Processor
import homework3.http.HttpResponse

import scala.concurrent.Future

object BrokenLinkDetector extends Processor[Set[String]] {
  def apply(url: String, response: HttpResponse): Future[Set[String]] = ???
}

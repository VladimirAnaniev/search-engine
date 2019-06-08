package homework3

import homework3.http._
import homework3.math.Monoid

import scala.concurrent.{ExecutionContext, Future}

case class SpideyConfig(maxDepth: Int,
                        sameDomainOnly: Boolean = true,
                        tolerateErrors: Boolean = true,
                        retriesOnError: Int = 0)

class Spidey(httpClient: HttpClient)(implicit ex: ExecutionContext) {
  def crawl[O: Monoid](url: String, config: SpideyConfig)
                      (processor: Processor[O]): Future[O] = {
    httpClient.get(url).flatMap(processor(url, _)) // TODO: not implemented fully yet.
  }
}

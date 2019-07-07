package tasks

import akka.actor.ActorSystem
import javax.inject.Inject
import play.api.Logging
import searchengine.{Spidey, SpideyConfig}
import searchengine.http.AsyncHttpClient
import searchengine.processors.WordCounter

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class CrawlerTask @Inject()(actorSystem: ActorSystem)(implicit executionContext: ExecutionContext) extends Logging {
  // TODO: These should be injected rather than instantiated here.
  val httpClient = new AsyncHttpClient
  val spidey = new Spidey(httpClient)


  actorSystem.scheduler.schedule(initialDelay = 10.seconds, interval = 10.minutes) {
    logger.info("Triggering Crawler task...")
    // TODO: Add proper processor
    spidey.crawl("https://en.wikipedia.org/wiki/Special:Random", SpideyConfig(10))(WordCounter).onComplete {
      case Success(_) => logger.info("Crawler task finished successfully")
      case Failure(err) => logger.error("Error occurred in crawler task", err)
    }
  }
}
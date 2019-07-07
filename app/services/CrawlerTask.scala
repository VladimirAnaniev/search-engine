package services

import akka.actor.ActorSystem
import javax.inject.Inject
import play.api.Logging
import play.api.db.slick.SlickApi
import searchengine.database.Database
import searchengine.{Spidey, SpideyConfig}
import searchengine.http.AsyncHttpClient
import searchengine.processors.{LinkDataProcessor, WordCounter}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

//import dbConfig.profile.api._

class CrawlerTask @Inject()(actorSystem: ActorSystem)(implicit executionContext: ExecutionContext) extends Logging {

  // TODO: These should be injected rather than instantiated here.
  val httpClient = new AsyncHttpClient
  val spidey = new Spidey(httpClient)


  actorSystem.scheduler.schedule(initialDelay = 10.seconds, interval = 10.minutes) {
    logger.info("Triggering Crawler task...")
    spidey.crawl("https://en.wikipedia.org/wiki/Special:Random", SpideyConfig(0))(LinkDataProcessor).onComplete {
      case Success(data) => {
        logger.info("Crawler task finished successfully")
        // TODO: Database should be injected
        Future.sequence(Database.addLinkDataToDatabase(data)).onComplete {
          case Success(data) => {
            val rowsInserted = data.sum
            logger.info(s"$rowsInserted rows inserted into database")
          }
          case Failure(err) => logger.error("Error inserting data into database", err)
        }
      }
      case Failure(err) => logger.error("Error occurred in crawler task", err)
    }
  }
}
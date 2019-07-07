package searchengine

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import searchengine.database.Database
import searchengine.database.Database._
import searchengine.http.AsyncHttpClient
import searchengine.processors.LinkDataProcessor

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration



class DatabaseTest extends FlatSpec with Matchers with ScalaFutures {

  "database" should "contain proper information" in {
//    Database.database.run(createTablesAction).flatMap { a =>
//    }
  }

}

object DatabaseTest extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  createTablesActions.map(Database.database.run).foreach(Await.ready(_, Duration.Inf))

  def getFutureResultBlocking[R](f: Future[R]) = Await.result(f, Duration.Inf)

  val httpClient = new AsyncHttpClient
  val spidey = new Spidey(httpClient)

  val result = getFutureResultBlocking(spidey
    .crawl("https://en.wikipedia.org/wiki/Adolf_Hitler",
      SpideyConfig(0))(LinkDataProcessor))

  Await.result(Future.sequence(Database.addLinkDataToDatabase(result)), Duration.Inf)

}

package services

import play.api.Logging
import searchengine.database.Database

import scala.concurrent.ExecutionContext

case class SearchService(implicit executionContext: ExecutionContext) extends Logging {
  def search(keyword: String) = Database.search(keyword)
}
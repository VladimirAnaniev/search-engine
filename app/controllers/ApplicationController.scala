package controllers

import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.SearchService

import scala.concurrent.ExecutionContext

class ApplicationController(cc: ControllerComponents,
                            wsClient: WSClient)
                           (implicit ec: ExecutionContext)extends AbstractController(cc) {
  val searchService = SearchService()

  // Can be both def or val
  def index: Action[AnyContent] = Action { request =>
    Ok(views.html.index(List.empty))
  }

  def search(keyword: String): Action[AnyContent] = Action.async {
    searchService.search(keyword).map(res => Ok(views.html.index(res.toList)))
  }
}
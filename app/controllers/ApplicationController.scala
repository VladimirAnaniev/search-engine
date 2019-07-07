package controllers

import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

class ApplicationController(cc: ControllerComponents,
                            wsClient: WSClient)
                           (implicit ec: ExecutionContext)extends AbstractController(cc) {
  // Can be both def or val
  def index = Action { request =>
    Ok(views.html.index())
  }

  def search(keyword: String) = Action { request =>

    Ok(views.html.index())
  }
}
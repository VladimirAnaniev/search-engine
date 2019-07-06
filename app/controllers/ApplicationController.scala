package controllers

import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class ApplicationController(cc: ControllerComponents,
                            wsClient: WSClient)
                           (implicit ec: ExecutionContext)extends AbstractController(cc) {
  // Can be both def or val
  def index = Action { request =>
//    Ok(views.html.index(42)(69))
    Ok(views.html.index())
  }
}
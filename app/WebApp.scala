import controllers.{ApplicationController, AssetsComponents}
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.api.routing.sird._
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext}
import play.filters.HttpFiltersComponents

class WebAppLoader extends ApplicationLoader {
  def load(context: Context): Application = new WebApp(context).application
}

class WebApp(context: Context)
  extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AhcWSComponents
    with AssetsComponents {

  override def httpFilters: Seq[EssentialFilter] = Seq.empty // disables filters from HttpFiltersComponents, like CSRF

  lazy val applicationController = new ApplicationController(controllerComponents, wsClient)

  val mainRoutes: Router.Routes = {
    case GET(p"/") => applicationController.index
    case GET(p"/assets/$file*") => assets.versioned(path = "/public", file = file)
  }

  lazy val router: Router = Router.from(mainRoutes)
}
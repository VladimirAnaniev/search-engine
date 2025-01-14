import controllers.{ApplicationController, AssetsComponents}
import play.api.ApplicationLoader.Context
import play.api.db.slick.SlickComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.api.routing.sird._
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext, LoggerConfigurator, Logging}
import play.filters.HttpFiltersComponents
import services.CrawlerTask

class WebAppLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    new WebApp(context).application
  }
}

class WebApp(context: Context)
  extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AhcWSComponents
    with AssetsComponents
//    with SlickComponents
    with Logging {

  override def httpFilters: Seq[EssentialFilter] = Seq.empty // disables filters from HttpFiltersComponents, like CSRF

  lazy val applicationController = new ApplicationController(controllerComponents, wsClient)

  val mainRoutes: Router.Routes = {
    case GET(p"/") => applicationController.index
    case GET(p"/search" ? q"keyword=$keyword") => applicationController.search(keyword)
    case GET(p"/assets/$file*") => assets.versioned(path = "/public", file = file)
  }

  private def initializeCrawlerTask(): Unit = {
    logger.info("Configuring Crawler task.")
    new CrawlerTask(actorSystem)
  }

  lazy val router: Router = Router.from(mainRoutes)
  initializeCrawlerTask()
}

package io.hydrosphere.serving.manager.api.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import io.hydrosphere.serving.manager.ManagerServices
import io.hydrosphere.serving.manager.config.ManagerConfiguration
import io.hydrosphere.serving.manager.api.http.controller.application.ApplicationController
import io.hydrosphere.serving.manager.api.http.controller.environment.HostSelectorController
import io.hydrosphere.serving.manager.api.http.controller.model.ModelController
import io.hydrosphere.serving.manager.api.http.controller.prometheus.PrometheusMetricsController
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._
import spray.json._

import scala.collection.immutable.Seq

class ManagerHttpApi(
  managerServices: ManagerServices,
  managerConfiguration: ManagerConfiguration
)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends Logging {

  val environmentController = new HostSelectorController(managerServices.hostSelectorService)

  val modelController = new ModelController(
    managerServices.modelManagementService,
    managerServices.modelVersionManagementService
  )

  val applicationController = new ApplicationController(managerServices.applicationManagementService)

  val prometheusMetricsController = new PrometheusMetricsController(managerServices.prometheusMetricsService)

  val swaggerController = new SwaggerDocController(
    Set(
      classOf[HostSelectorController],
      classOf[ModelController],
      classOf[ApplicationController],
      classOf[PrometheusMetricsController]
    ),
    "2"
  )

  val commonExceptionHandler = ExceptionHandler {
    case p: Throwable =>
      logger.error(p.getMessage, p)
      throw p
      complete(
        HttpResponse(
          StatusCodes.InternalServerError,
          entity = Map(
            "error" -> "InternalUncatched",
            "information" -> Option(p.getMessage).getOrElse("Unknown error (exception message == null)")
          ).toJson.toString()
        )
      )
  }

  val controllerRoutes: Route = pathPrefix("v2") {
    handleExceptions(commonExceptionHandler) {
      swaggerController.routes ~
        modelController.routes ~
        applicationController.routes ~
        prometheusMetricsController.routes ~
        environmentController.routes ~
        path("health") {
          complete {
            "OK"
          }
        }
    }
  }

  def routes = CorsDirectives.cors(
    CorsSettings.defaultSettings.copy(allowedMethods = Seq(GET, POST, HEAD, OPTIONS, PUT, DELETE))
  ) {
    pathPrefix("api") {
      controllerRoutes
    } ~ pathPrefix("swagger") {
      path(Segments) { segs =>
        val path = segs.mkString("/")
        getFromResource(s"swagger/$path")
      }
    }
  }

  val serverBinding = Http().bindAndHandle(routes, "0.0.0.0", managerConfiguration.application.port)
}
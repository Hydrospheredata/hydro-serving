package io.hydrosphere.serving.manager

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import io.hydrosphere.serving.manager.controller._
import akka.http.scaladsl.server.Directives.{path, _}
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import io.hydrosphere.serving.manager.controller.application.ApplicationController
import io.hydrosphere.serving.manager.controller.environment.EnvironmentController
import io.hydrosphere.serving.manager.controller.model.ModelController
import io.hydrosphere.serving.manager.controller.model_source.ModelSourceController
import io.hydrosphere.serving.manager.controller.prometheus.PrometheusMetricsController
import io.hydrosphere.serving.manager.controller.runtime.RuntimeController
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

class ManagerHttpApi(
  managerServices: ManagerServices,
  managerConfiguration: ManagerConfiguration
)(
  implicit val system: ActorSystem,
  implicit val ex: ExecutionContext,
  implicit val materializer: ActorMaterializer
) extends Logging {

  val environmentController = new EnvironmentController(managerServices.environmentManagementService)

  val runtimeController = new RuntimeController(managerServices.runtimeManagementService)

  val modelSourceController = new ModelSourceController(managerServices.sourceManagementService)

  val modelController = new ModelController(
    managerServices.modelManagementService,
    managerServices.aggregatedInfoUtilityService,
    managerServices.modelBuildManagmentService,
    managerServices.modelVersionManagementService
  )

  val serviceController = new ServiceController(
    managerServices.serviceManagementService,
    managerServices.applicationManagementService
  )

  val applicationController = new ApplicationController(managerServices.applicationManagementService)

  val prometheusMetricsController = new PrometheusMetricsController(managerServices.prometheusMetricsService)

  val swaggerController = new SwaggerDocController(
    Set(
      classOf[EnvironmentController],
      classOf[RuntimeController],
      classOf[ModelController],
      classOf[ServiceController],
      classOf[ApplicationController],
      classOf[PrometheusMetricsController],
      classOf[ModelSourceController]
    )
  )

  val commonExceptionHandler = ExceptionHandler {
    case p: Throwable =>
      logger.error(p.getMessage, p)
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

  val routes: Route = CorsDirectives.cors(
    CorsSettings.defaultSettings.copy(allowedMethods = Seq(GET, POST, HEAD, OPTIONS, PUT, DELETE))
  ) {
    handleExceptions(commonExceptionHandler) {
      swaggerController.routes ~
        modelController.routes ~
        serviceController.routes ~
        applicationController.routes ~
        runtimeController.routes ~
        prometheusMetricsController.routes ~
        environmentController.routes ~
        modelSourceController.routes ~
        pathPrefix("swagger") {
          path(Segments) { segs =>
            val path = segs.mkString("/")
            getFromResource(s"swagger/$path")
          }
        } ~ path("health") {
        complete {
          "OK"
        }
      }
    }
  }

  val serverBinding = Http().bindAndHandle(routes, "0.0.0.0", managerConfiguration.application.port)
}
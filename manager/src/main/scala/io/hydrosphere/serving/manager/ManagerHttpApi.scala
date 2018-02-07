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
import io.hydrosphere.serving.manager.controller.model_source.ModelSourceController
import io.hydrosphere.serving.manager.controller.prometheus.PrometheusMetricsController
import org.apache.logging.log4j.scala.Logging

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class ManagerHttpApi(
  managerServices: ManagerServices,
  managerConfiguration: ManagerConfiguration
)(
  implicit val system: ActorSystem,
  implicit val ex: ExecutionContext,
  implicit val materializer: ActorMaterializer
) extends Logging {

  val environmentController = new EnvironmentController(managerServices.serviceManagementService)

  val runtimeController = new RuntimeController(managerServices.runtimeManagementService)

  val modelSourceController = new ModelSourceController(managerServices.sourceManagementService)

  val modelController = new ModelController(managerServices.modelManagementService)

  val serviceController = new ServiceController(
    managerServices.serviceManagementService,
    managerServices.applicationManagementService
  )

  val applicationController = new ApplicationController(managerServices.applicationManagementService)

  val prometheusMetricsController = new PrometheusMetricsController(managerServices.prometheusMetricsService)

  val swaggerController = new SwaggerDocController(system) {
    override val apiTypes: Seq[ru.Type] = Seq(
      ru.typeOf[EnvironmentController],
      ru.typeOf[RuntimeController],
      ru.typeOf[ModelController],
      ru.typeOf[ServiceController],
      ru.typeOf[ApplicationController],
      ru.typeOf[PrometheusMetricsController],
      ru.typeOf[ModelSourceController]
    )
  }

  private def getExceptionMessage(p: Throwable): String = {
    if (p.getMessage() == null) {
      "Unknown error"
    } else {
      p.getMessage
    }
  }

  val commonExceptionHandler = ExceptionHandler {
    case x: IllegalArgumentException =>
      logger.error(x.getMessage, x)
      complete(HttpResponse(StatusCodes.BadRequest, entity = getExceptionMessage(x)))
    case p: Throwable =>
      logger.error(p.getMessage, p)
      complete(HttpResponse(StatusCodes.InternalServerError, entity = getExceptionMessage(p)))
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

  val serverBinding=Http().bindAndHandle(routes, "0.0.0.0", managerConfiguration.application.port)
}
package io.hydrosphere.serving.manager

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpResponse, ResponseEntity, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import io.hydrosphere.serving.manager.controller.{ModelRuntimeController, SwaggerDocController, _}
import akka.http.scaladsl.server.Directives.{path, _}
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import io.hydrosphere.serving.manager.controller.prometheus.PrometheusMetricsController
import io.hydrosphere.serving.manager.controller.ui.{UISpecificController, UISpecificRuntimeController, UISpecificWeightServiceController}
import org.apache.logging.log4j.scala.Logging

import scala.Option
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class ManagerApi(
  managerServices: ManagerServices,
  managerConfiguration: ManagerConfiguration
)(
  implicit val system: ActorSystem,
  implicit val ex: ExecutionContext,
  implicit val materializer: ActorMaterializer
) extends Logging {

  val runtimeTypeController = new RuntimeTypeController(managerServices.modelManagementService, managerServices.runtimeTypeManagementService)

  val modelController = new ModelController(managerServices.modelManagementService)

  val modelRuntimeController = new ModelRuntimeController(managerServices.modelManagementService)

  val modelServiceController = new ModelServiceController(managerServices.runtimeManagementService, managerServices.servingManagementService)

  val applicationController = new ApplicationController(managerServices.servingManagementService)

  val prometheusMetricsController = new PrometheusMetricsController(managerServices.prometheusMetricsService)

  val uiSpecificController = new UISpecificController(managerServices.uiManagementService)

  val uiSpecificWeightServiceController = new UISpecificWeightServiceController(managerServices.uiManagementService)

  val uiSpecificRuntimeController = new UISpecificRuntimeController(managerServices.uiManagementService)

  val servingEnvironmentController = new ServingEnvironmentController(managerServices.runtimeManagementService)

  val modelSourceController = new ModelSourceController(managerServices.sourceManagementService)

  val swaggerController = new SwaggerDocController(system) {
    override val apiTypes: Seq[ru.Type] = Seq(
      ru.typeOf[ServingEnvironmentController],
      ru.typeOf[RuntimeTypeController],
      ru.typeOf[ModelController],
      ru.typeOf[ModelRuntimeController],
      ru.typeOf[ModelServiceController],
      ru.typeOf[ApplicationController],
      ru.typeOf[PrometheusMetricsController],
      ru.typeOf[ModelSourceController],
      ru.typeOf[UISpecificController],
      ru.typeOf[UISpecificWeightServiceController],
      ru.typeOf[UISpecificRuntimeController]
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
        modelRuntimeController.routes ~
        modelServiceController.routes ~
        applicationController.routes ~
        runtimeTypeController.routes ~
        prometheusMetricsController.routes ~
        uiSpecificController.routes ~
        uiSpecificWeightServiceController.routes ~
        servingEnvironmentController.routes ~
        modelSourceController.routes ~
        uiSpecificRuntimeController.routes ~
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
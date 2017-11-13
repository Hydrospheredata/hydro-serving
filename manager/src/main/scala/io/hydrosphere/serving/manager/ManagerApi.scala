package io.hydrosphere.serving.manager

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpResponse, ResponseEntity, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import io.hydrosphere.serving.controller.{CommonController, SwaggerDocController}
import io.hydrosphere.serving.manager.controller.{ModelRuntimeController, _}
import akka.http.scaladsl.server.Directives.{path, _}
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import io.hydrosphere.serving.manager.controller.envoy.EnvoyManagementController
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
class ManagerApi(managerServices: ManagerServices)
  (implicit val system: ActorSystem, implicit val ex: ExecutionContext) extends Logging {

  val commonController = new CommonController

  val runtimeTypeController = new RuntimeTypeController(managerServices.modelManagementService)

  val modelController = new ModelController(managerServices.modelManagementService)

  val modelRuntimeController = new ModelRuntimeController(managerServices.modelManagementService)

  val modelServiceController = new ModelServiceController(managerServices.runtimeManagementService, managerServices.servingManagementService)

  val applicationController = new ApplicationController(managerServices.servingManagementService)

  val envoyManagementController = new EnvoyManagementController(managerServices.envoyManagementService)

  val prometheusMetricsController = new PrometheusMetricsController(managerServices.prometheusMetricsService)

  val uiSpecificController = new UISpecificController(managerServices.uiManagementService)

  val uiSpecificWeightServiceController = new UISpecificWeightServiceController(managerServices.uiManagementService)

  val uiSpecificRuntimeController = new UISpecificRuntimeController(managerServices.uiManagementService)

  val servingEnvironmentController = new ServingEnvironmentController(managerServices.runtimeManagementService)

  val swaggerController = new SwaggerDocController(system) {
    override val apiTypes: Seq[ru.Type] = Seq(
      ru.typeOf[ServingEnvironmentController],
      ru.typeOf[RuntimeTypeController],
      ru.typeOf[ModelController],
      ru.typeOf[ModelRuntimeController],
      ru.typeOf[ModelServiceController],
      ru.typeOf[EnvoyManagementController],
      ru.typeOf[ApplicationController],
      ru.typeOf[PrometheusMetricsController],
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
      commonController.routes ~
        swaggerController.routes ~
        modelController.routes ~
        modelRuntimeController.routes ~
        modelServiceController.routes ~
        applicationController.routes ~
        runtimeTypeController.routes ~
        envoyManagementController.routes ~
        prometheusMetricsController.routes ~
        uiSpecificController.routes ~
        uiSpecificWeightServiceController.routes ~
        servingEnvironmentController.routes ~
        uiSpecificRuntimeController.routes
    }
  }
}
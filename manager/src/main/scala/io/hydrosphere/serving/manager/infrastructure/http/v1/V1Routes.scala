package io.hydrosphere.serving.manager.infrastructure.http.v1

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.manager.ManagerServices
import io.hydrosphere.serving.manager.config.ManagerConfiguration
import io.hydrosphere.serving.manager.infrastructure.http.SwaggerDocController
import io.hydrosphere.serving.manager.infrastructure.http.v1.controller.ServiceController
import io.hydrosphere.serving.manager.infrastructure.http.v1.controller.application.ApplicationController
import io.hydrosphere.serving.manager.infrastructure.http.v1.controller.environment.EnvironmentController
import io.hydrosphere.serving.manager.infrastructure.http.v1.controller.model.ModelController
import io.hydrosphere.serving.manager.infrastructure.http.v1.controller.prometheus.PrometheusMetricsController
import io.hydrosphere.serving.manager.infrastructure.http.v1.controller.runtime.RuntimeController
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
import org.apache.logging.log4j.scala.Logging
import spray.json._

class V1Routes(
  managerServices: ManagerServices,
  managerConfiguration: ManagerConfiguration
)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends Logging {

  val environmentController = new EnvironmentController(managerServices.environmentManagementService)

  val runtimeController = new RuntimeController(managerServices.runtimeManagementService)

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
      classOf[PrometheusMetricsController]
    ),
    "1"
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

  val routes: Route = pathPrefix("v1") {
    handleExceptions(commonExceptionHandler) {
      swaggerController.routes ~
        modelController.routes ~
        serviceController.routes ~
        applicationController.routes ~
        runtimeController.routes ~
        prometheusMetricsController.routes ~
        environmentController.routes ~
        path("health") {
        complete {
          "OK"
        }
      }
    }
  }
}
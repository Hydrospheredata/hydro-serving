package io.hydrosphere.serving.manager.api.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import cats.effect.Effect
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import io.hydrosphere.serving.BuildInfo
import io.hydrosphere.serving.manager.api.http.controller.host_selector.HostSelectorController
import io.hydrosphere.serving.manager.api.http.controller.{AkkaHttpControllerDsl, ApplicationController, SwaggerDocController}
import io.hydrosphere.serving.manager.{ManagerRepositories, ManagerServices}
import io.hydrosphere.serving.manager.config.ManagerConfiguration
import io.hydrosphere.serving.manager.api.http.controller.model.ModelController
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._
import spray.json._

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

class HttpApiServer[F[_]: Effect](
  managerRepositories: ManagerRepositories[F],
  managerServices: ManagerServices[F],
  managerConfiguration: ManagerConfiguration
)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val ec: ExecutionContext
) extends AkkaHttpControllerDsl {

  val environmentController = new HostSelectorController[F](
    managerServices.hostSelectorService,
    managerRepositories.hostSelectorRepository
  )

  val modelController = new ModelController[F](
    managerServices.modelService,
    managerRepositories.modelRepository,
    managerServices.versionService
  )

  val applicationController = new ApplicationController(
    managerServices.appService,
    managerRepositories.applicationRepository
  )

  val swaggerController = new SwaggerDocController(
    Set(
      classOf[HostSelectorController[F]],
      classOf[ModelController[F]],
      classOf[ApplicationController[F]]
    ),
    "2"
  )

  val controllerRoutes: Route = pathPrefix("v2") {
    handleExceptions(commonExceptionHandler) {
      swaggerController.routes ~
        modelController.routes ~
        applicationController.routes ~
        environmentController.routes
    }
  }

  def routes: Route = CorsDirectives.cors(
    CorsSettings.defaultSettings.copy(allowedMethods = Seq(GET, POST, HEAD, OPTIONS, PUT, DELETE))
  ) {
    pathPrefix("health") { complete("OK") } ~
    pathPrefix("api") {
      controllerRoutes ~
        pathPrefix("buildinfo") { complete(BuildInfo.toJson) }
    } ~ pathPrefix("swagger") {
      path(Segments) { segs =>
        val path = segs.mkString("/")
        getFromResource(s"swagger/$path")
      }
    }
  }

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(routes, "0.0.0.0", managerConfiguration.application.port)
  }
}
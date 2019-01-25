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
import io.hydrosphere.serving.manager.api.http.controller.{ApplicationController, SwaggerDocController}
import io.hydrosphere.serving.manager.{ManagerRepositories, ManagerServices}
import io.hydrosphere.serving.manager.config.ManagerConfiguration
import io.hydrosphere.serving.manager.api.http.controller.environment.HostSelectorController
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
) extends Logging {

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

  val commonExceptionHandler = ExceptionHandler {
    case DeserializationException(msg,_, fields) =>
      logger.error(msg)
      complete(
        HttpResponse(
          StatusCodes.BadRequest,
          entity = Map(
            "error" -> "RequestDeserializationError",
            "message" -> msg,
            "fields" -> fields
          ).asInstanceOf[Map[String, Any]].toJson.toString()
        )
      )
    case p: SerializationException =>
      logger.error(p.getMessage, p)
      complete(
        HttpResponse(
          StatusCodes.InternalServerError,
          entity = Map(
            "error" -> "ResponseSerializationException",
            "message" -> Option(p.getMessage).getOrElse(s"Unknown error: $p")
          ).toJson.toString()
        )
      )
    case p: Throwable =>
      logger.error(p.getMessage, p)
      complete(
        HttpResponse(
          StatusCodes.InternalServerError,
          entity = Map(
            "error" -> "InternalException",
            "message" -> Option(p.getMessage).getOrElse(s"Unknown error: $p")
          ).toJson.toString()
        )
      )
  }

  val controllerRoutes: Route = pathPrefix("v2") {
    handleExceptions(commonExceptionHandler) {
      swaggerController.routes ~
        modelController.routes ~
        applicationController.routes ~
        environmentController.routes ~
        path("health") {
          complete {
            "OK"
          }
        }
    }
  }

  def routes: Route = CorsDirectives.cors(
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

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(routes, "0.0.0.0", managerConfiguration.application.port)
  }
}
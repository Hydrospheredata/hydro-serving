package io.hydrosphere.serving.manager.infrastructure.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Directives.{path, _}
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import io.hydrosphere.serving.manager.ManagerServices
import io.hydrosphere.serving.manager.config.ManagerConfiguration
import io.hydrosphere.serving.manager.infrastructure.http.v1.V1Routes
import io.hydrosphere.serving.manager.infrastructure.http.v2.V2Routes
import org.apache.logging.log4j.scala.Logging

import scala.collection.immutable.Seq

class ManagerHttpApi(
  managerServices: ManagerServices,
  managerConfiguration: ManagerConfiguration
)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends Logging {

  val apiV1 = new V1Routes(managerServices, managerConfiguration)
  val apiV2 = new V2Routes()

  def routes = CorsDirectives.cors(
    CorsSettings.defaultSettings.copy(allowedMethods = Seq(GET, POST, HEAD, OPTIONS, PUT, DELETE))
  ) {
    pathPrefix("api") {
      apiV1.routes ~
        apiV2.routes
    } ~ pathPrefix("swagger") {
      path(Segments) { segs =>
        val path = segs.mkString("/")
        getFromResource(s"swagger/$path")
      }
    }
  }

  val serverBinding = Http().bindAndHandle(routes, "0.0.0.0", managerConfiguration.application.port)
}
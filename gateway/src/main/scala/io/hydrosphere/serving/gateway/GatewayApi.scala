package io.hydrosphere.serving.gateway

import scala.reflect.runtime.{universe => ru}
import akka.actor.{ActorRef, ActorSystem}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import io.hydrosphere.serving.gateway.controller.ServeController
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.hydrosphere.serving.controller.{CommonController, SwaggerDocController}


/**
  *
  */
class GatewayApi(serveActor: ActorRef) (implicit val system: ActorSystem){

  val swaggerController = new SwaggerDocController(system) {
    override val apiClasses = Set(classOf[ServeController])
  }

  val commonController = new CommonController()

  val serveController = new ServeController(serveActor)

  val routes: Route = {
    commonController.routes ~ swaggerController.routes ~ CorsDirectives.cors() {
      serveController.routes
    }
  }
}

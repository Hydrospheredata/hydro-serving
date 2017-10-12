package io.hydrosphere.serving.streaming

import scala.reflect.runtime.{universe => ru}
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.hydrosphere.serving.controller.{CommonController, SwaggerDocController}

/**
  *
  */
class StreamingKafkaApi () (implicit val system: ActorSystem){

  val swaggerController = new SwaggerDocController(system) {
    override val apiTypes: Seq[ru.Type] = Seq(/*ru.typeOf[ServeController]*/)
  }

  val commonController = new CommonController()

  val routes: Route = {
    commonController.routes ~ swaggerController.routes /*~ CorsDirectives.cors() {
      serveController.routes
    }*/
  }
}


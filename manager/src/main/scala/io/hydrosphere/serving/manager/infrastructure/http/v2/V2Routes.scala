package io.hydrosphere.serving.manager.infrastructure.http.v2

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.manager.infrastructure.http.SwaggerDocController

class V2Routes()(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) {
  val swaggerController = new SwaggerDocController(
    Set.empty,
    "2"
  )

  val routes = pathPrefix("v2") {
    swaggerController.routes
  }
}

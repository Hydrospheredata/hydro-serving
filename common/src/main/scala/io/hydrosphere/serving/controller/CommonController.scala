package io.hydrosphere.serving.controller

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route


/**
  *META-INF/resources/webjars/swagger-ui/3.0.18/index.html
  */
class CommonController {
  val routes: Route = {
    path("swagger" / Segment) { name =>
      getFromResource(s"swagger/$name")
    } ~ path("health") {
      complete {
        "OK"
      }
    }
  }
}

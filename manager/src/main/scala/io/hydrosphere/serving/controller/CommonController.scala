package io.hydrosphere.serving.controller

import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route


/**
  *
  */
class CommonController {
  val routes: Route = {
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

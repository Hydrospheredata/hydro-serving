package io.hydrosphere.serving.gateway.controller

import akka.actor.ActorRef
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern._

import scala.util.{Failure, Success}
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.util.Timeout
import scala.concurrent.duration._

class ServeController(serveActor: ActorRef) {
  implicit val timeout = Timeout(5.minutes)

  def serve = path("/api/v1/serve") {
    get {
      extractRequest { request =>
        onComplete((serveActor ? request).mapTo[HttpResponse]) {
          case Success(response) => complete(response)
          case Failure(ex) =>
            complete((InternalServerError, s"Error on server side: ${ex.getMessage}"))
        }
      }
    }
  }

  val routes: Route = serve
}
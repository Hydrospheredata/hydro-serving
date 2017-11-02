package io.hydrosphere.serving.gateway.controller

import akka.actor.ActorRef
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern._

import scala.util.{Failure, Success}
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.util.Timeout
import io.hydrosphere.serving.controller.{ServingDataDirectives, TracingHeaders}
import io.hydrosphere.serving.gateway.actor.{ServeError, ServeRequest, ServeResponse}
import io.hydrosphere.serving.model.CommonJsonSupport

import scala.concurrent.duration._

class ServeController(serveActor: ActorRef) extends CommonJsonSupport
  with ServingDataDirectives {

  implicit val timeout = Timeout(5.minutes)

  def serve = path("api" / "v1" / "serve" / Segment) { endpointName=>
    post {
      extractRequest { request =>
        extractRawData { bytes =>
          val serveRequest = ServeRequest(
            endpointName = endpointName,
            tracingHeaders = request.headers.filter(h => TracingHeaders.isTracingHeaderName(h.name())),
            request = bytes
          )
          onComplete(serveActor ? serveRequest){
            case Success(response) => response match {
              case res:ServeResponse => complete(res.response)
              case err:ServeError => complete((err.statusCode, err.errorMessage))
            }
            case Failure(ex) =>
              complete((InternalServerError, s"Error on server side: ${ex.getMessage}"))
          }
        }
      }
    }
  }

  val routes: Route = serve
}
package io.hydrosphere.serving.gateway.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.model._
import io.hydrosphere.serving.connector._
import io.hydrosphere.serving.model.Endpoint

import scala.util.{Failure, Success}


case class UpdateEndpoints(
  endpoints: Map[String, Endpoint]
)

case class ServeRequest(
  tracingHeaders: Seq[HttpHeader],
  request: Seq[Any],
  endpointName: String
)

case class ServeResponse(
  tracingHeaders: Seq[HttpHeader],
  response: Seq[Any]
)

case class ServeError(
  tracingHeaders: Seq[HttpHeader],
  errorMessage: String,
  statusCode: StatusCode
)

class ServeActor(sidecarConnector: RuntimeMeshConnector) extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher

  var endpoints: Map[String, Endpoint] = Map()

  override def receive = {
    case UpdateEndpoints(indexed) => this.endpoints = indexed

    case request: ServeRequest =>
      val currentSender = sender()
      val endpoint = endpoints.get(request.endpointName)
      if (endpoint.isEmpty || endpoint.get.currentPipeline.isEmpty) {
        currentSender ! ServeError(
          tracingHeaders = request.tracingHeaders,
          statusCode = StatusCodes.BadRequest,
          errorMessage = "Wrong Endpoint name"
        )
      } else {
        val pipeline = endpoint.get.currentPipeline.get.stages
          .map(s => ExecutionUnit(
            serviceName = s.serviceName,
            servicePath = s.servePath
          ))


        sidecarConnector.execute(ExecutionCommand(
          headers = request.tracingHeaders,
          json = request.request,
          pipeline
        )).onComplete({
          case Success(res) =>
            currentSender ! ServeResponse(
              tracingHeaders = request.tracingHeaders,
              response = res.json
            )
          case Failure(ex) =>
            log.error(ex, ex.getMessage)
            currentSender ! ServeError(
              tracingHeaders = request.tracingHeaders,
              statusCode = StatusCodes.InternalServerError,
              errorMessage = ex.getMessage
            )
        })
      }
  }
}

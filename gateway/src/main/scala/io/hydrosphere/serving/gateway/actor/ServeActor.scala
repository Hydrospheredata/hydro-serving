package io.hydrosphere.serving.gateway.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.model._
import io.hydrosphere.serving.connector._
import io.hydrosphere.serving.model.Application

import scala.util.{Failure, Success}


case class UpdateEndpoints(
  appications: Map[String, Application]
)

case class ServeRequest(
  tracingHeaders: Seq[HttpHeader],
  request: Array[Byte],
  endpointName: String
)

case class ServeResponse(
  tracingHeaders: Seq[HttpHeader],
  response: Array[Byte]
)

case class ServeError(
  tracingHeaders: Seq[HttpHeader],
  errorMessage: String,
  statusCode: StatusCode
)

class ServeActor(sidecarConnector: RuntimeMeshConnector) extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher

  var endpoints: Map[String, Application] = Map()

  override def receive = {
    case UpdateEndpoints(indexed) => this.endpoints = indexed

    case request: ServeRequest =>
      val currentSender = sender()
      val endpoint = endpoints.get(request.endpointName)
      if (endpoint.isEmpty || endpoint.get.executionGraph.stages.isEmpty) {
        currentSender ! ServeError(
          tracingHeaders = request.tracingHeaders,
          statusCode = StatusCodes.BadRequest,
          errorMessage = "Wrong Endpoint name"
        )
      } else {
        val pipeline = endpoint.get.executionGraph.stages.indices
          .map(s => ExecutionUnit(
            serviceName = s"app${endpoint.get.id}stage$s",
            servicePath = "/serve"
          ))


        sidecarConnector.execute(ExecutionCommand(
          headers = request.tracingHeaders,
          json = request.request,
          pipeline
        )).onComplete(result => {
          val message = result match {
            case Success(ExecutionSuccess(data)) =>
              ServeResponse(request.tracingHeaders, data)

            case Success(ExecutionFailure(err, code)) =>
              log.error(new RuntimeException("Serving failed"), err)
              ServeError(request.tracingHeaders, err, code)

            case Failure(ex) =>
              log.error(ex, ex.getMessage)
              ServeError(request.tracingHeaders, ex.getMessage, StatusCodes.InternalServerError)
          }
          currentSender ! message
        })
      }
  }
}

package io.hydrosphere.serving.gateway.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.model._
import io.hydrosphere.serving.gateway.connector.{SidecarConnector, SingleExecutionCommand}
import io.hydrosphere.serving.model.Endpoint
import io.hydrosphere.serving.controller.TracingHeaders

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

class ServeActor(sidecarConnector: SidecarConnector) extends Actor with ActorLogging {
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
        val pipeline = endpoint.get.currentPipeline.get

        val stage = pipeline.stages.head

        //TODO Move to streams
        //TODO avoid Serialization/Deserialization, work with http entry
        val fSerialized = {
          var fAccum = sidecarConnector.execute(SingleExecutionCommand(
            headers = request.tracingHeaders,
            json = request.request,
            runtimeName = stage.serviceName,
            runtimePath = stage.servePath
          ))
          for(item <- pipeline.stages.drop(1)) {
            fAccum = fAccum.flatMap(res=>{
              sidecarConnector.execute(SingleExecutionCommand(
                headers = request.tracingHeaders,
                json = res.json,
                runtimeName = item.serviceName,
                runtimePath = item.servePath
              ))
            })
          }
          fAccum
        }

        fSerialized.onComplete({
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

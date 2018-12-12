package io.hydrosphere.serving.manager.api.http.controller.application

case class ExecutionStepRequest(
  services: List[ServiceCreationDescription]
)

object ExecutionStepRequest {
  import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._
  implicit val format = jsonFormat1(ExecutionStepRequest.apply)
}
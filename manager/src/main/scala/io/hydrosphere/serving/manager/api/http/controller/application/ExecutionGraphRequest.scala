package io.hydrosphere.serving.manager.api.http.controller.application

case class ExecutionGraphRequest(
  stages: Seq[ExecutionStepRequest]
)

object ExecutionGraphRequest {
  import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._
  implicit val format = jsonFormat1(ExecutionGraphRequest.apply)
}
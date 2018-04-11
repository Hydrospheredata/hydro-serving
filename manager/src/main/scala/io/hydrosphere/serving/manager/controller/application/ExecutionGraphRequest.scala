package io.hydrosphere.serving.manager.controller.application

case class ExecutionGraphRequest(
  stages: Seq[ExecutionStepRequest]
)

object ExecutionGraphRequest {
  import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
  implicit val format = jsonFormat1(ExecutionGraphRequest.apply)
}
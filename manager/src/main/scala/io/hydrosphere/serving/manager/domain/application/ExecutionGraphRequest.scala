package io.hydrosphere.serving.manager.domain.application

case class ExecutionGraphRequest(
  stages: List[PipelineStageRequest]
)

object ExecutionGraphRequest {

  import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._

  implicit val format = jsonFormat1(ExecutionGraphRequest.apply)
}
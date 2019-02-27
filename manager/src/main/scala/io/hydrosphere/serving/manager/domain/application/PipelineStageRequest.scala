package io.hydrosphere.serving.manager.domain.application

case class PipelineStageRequest(
  modelVariants: Seq[ModelVariantRequest]
)

object PipelineStageRequest {
  import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._
  implicit val format = jsonFormat1(PipelineStageRequest.apply)
}
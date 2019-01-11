package io.hydrosphere.serving.manager.domain.application

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion

case class PipelineStage(
  services: Seq[PipelineStageNode],
  signature: Option[ModelSignature],
)

case class PipelineStageNode(
  modelVersion: ModelVersion,
  weight: Int,
  signature: Option[ModelSignature]
)

object PipelineStage {
  def stageId(applicationId: Long, stageIndex: Int): String =
    s"app${applicationId}stage$stageIndex"
}
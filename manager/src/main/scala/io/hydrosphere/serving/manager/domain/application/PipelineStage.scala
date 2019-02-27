package io.hydrosphere.serving.manager.domain.application

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion

case class PipelineStage(
  modelVariants: Seq[ModelVariant],
  signature: ModelSignature,
)

object PipelineStage {
  def stageId(applicationId: Long, stageIndex: Int): String =
    s"app${applicationId}stage$stageIndex"
}
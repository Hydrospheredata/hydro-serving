package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.contract.model_signature.ModelSignature

case class ApplicationStage(
  services: List[WeightedService],
  signature: Option[ModelSignature]
)

object ApplicationStage {
  def stageId(applicationId: Long, stageIndex: Int): String =
    s"app${applicationId}stage$stageIndex"
}
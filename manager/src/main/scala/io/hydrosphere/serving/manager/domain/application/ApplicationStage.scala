package io.hydrosphere.serving.manager.domain.application

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion

case class ApplicationStage(
  services: List[DetailedServiceDescription],
  signature: Option[ModelSignature],
)

case class DetailedServiceDescription(
  modelVersion: ModelVersion,
  weight: Int,
  signature: Option[ModelSignature]
)

object ApplicationStage {
  def stageId(applicationId: Long, stageIndex: Int): String =
    s"app${applicationId}stage$stageIndex"
}
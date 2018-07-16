package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.DataProfileFields

case class ApplicationStage(
  services: List[WeightedService],
  signature: Option[ModelSignature],
  dataProfileFields: DataProfileFields
)

object ApplicationStage {
  def stageId(applicationId: Long, stageIndex: Int): String =
    s"app${applicationId}stage$stageIndex"
}
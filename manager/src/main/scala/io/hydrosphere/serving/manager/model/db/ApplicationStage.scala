package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.DataProfileFields

case class ApplicationStage(
  services: List[DetailedServiceDescription],
  signature: Option[ModelSignature],
  dataProfileFields: DataProfileFields
)

case class DetailedServiceDescription(
  runtime: Runtime,
  modelVersion: ModelVersion,
  environment: Environment,
  weight: Int,
  signature: Option[ModelSignature]
) {
  def serviceDescription = ServiceKeyDescription(
    runtimeId = runtime.id,
    modelVersionId = Some(modelVersion.id),
    environmentId = Some(environment.id)
  )
}

object ApplicationStage {
  def stageId(applicationId: Long, stageIndex: Int): String =
    s"app${applicationId}stage$stageIndex"
}

package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.grpc.applications.ExecutionService
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

  def toGrpc = ExecutionService(
    runtime = Some(runtime.toGrpc),
    modelVersion = Some(modelVersion.toGrpc),
    environment = Some(environment.toGrpc),
    weight = weight
  )
}

object ApplicationStage {
  def stageId(applicationId: Long, stageIndex: Int): String =
    s"app${applicationId}stage$stageIndex"
}

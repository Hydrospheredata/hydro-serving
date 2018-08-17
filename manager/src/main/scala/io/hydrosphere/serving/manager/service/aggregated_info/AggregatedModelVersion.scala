package io.hydrosphere.serving.manager.service.aggregated_info

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.{Application, Model, ModelVersion}

case class AggregatedModelVersion(
  id: Long,
  imageName: String,
  imageTag: String,
  imageSHA256: String,
  created: LocalDateTime,
  modelName: String,
  modelVersion: Long,
  modelType: ModelType,
  model: Option[Model],
  modelContract: ModelContract,
  applications: Seq[Application]
)

object AggregatedModelVersion {
  def fromModelVersion(modelVersion: ModelVersion, apps: Seq[Application]): AggregatedModelVersion = {
    AggregatedModelVersion(
      id = modelVersion.id,
      imageName = modelVersion.imageName,
      imageTag = modelVersion.imageTag,
      imageSHA256 = modelVersion.imageSHA256,
      created = modelVersion.created,
      modelName = modelVersion.modelName,
      modelVersion = modelVersion.modelVersion,
      modelType = modelVersion.modelType,
      model = modelVersion.model,
      modelContract = modelVersion.modelContract,
      applications = apps
    )
  }
}
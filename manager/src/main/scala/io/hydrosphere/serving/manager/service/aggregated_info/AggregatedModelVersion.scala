package io.hydrosphere.serving.manager.service.aggregated_info

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.domain.application.Application
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.model.api.ModelType

case class AggregatedModelVersion(
  id: Long,
  imageName: String,
  imageTag: String,
  imageSHA256: String,
  created: LocalDateTime,
  finished: Option[LocalDateTime],
  status: String,
  modelId: Long,
  modelName: String,
  modelVersion: Long,
  modelType: ModelType,
  modelContract: ModelContract,
  applications: Seq[String]
)

object AggregatedModelVersion {
  def fromModelVersion(modelVersion: ModelVersion, apps: Seq[Application]): AggregatedModelVersion = {
    AggregatedModelVersion(
      id = modelVersion.id,
      imageName = modelVersion.imageName,
      imageTag = modelVersion.imageTag,
      imageSHA256 = modelVersion.imageSHA256,
      created = modelVersion.created,
      finished = modelVersion.finished,
      status = modelVersion.status,
      modelName = modelVersion.model.name,
      modelType = modelVersion.modelType,
      modelContract = modelVersion.modelContract,
      applications = apps.map(_.name),
      modelVersion = modelVersion.modelVersion,
      modelId = modelVersion.model.id
    )
  }
}
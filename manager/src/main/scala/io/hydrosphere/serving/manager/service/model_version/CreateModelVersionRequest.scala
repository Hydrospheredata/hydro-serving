package io.hydrosphere.serving.manager.service.model_version

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.{Model, ModelVersion}

case class CreateModelVersionRequest(
  imageName: String,
  imageTag: String,
  imageSHA256: String,
  modelName: String,
  modelVersion: Long,
  source: Option[String],
  runtimeTypeId: Option[Long],
  modelContract: ModelContract,
  modelId: Option[Long],
  tags: Option[List[String]],
  configParams: Option[Map[String, String]],
  modelType: String
) {

  def toModelVersion(model: Model): ModelVersion = {
    ModelVersion(
      id = 0,
      imageName = this.imageName,
      imageTag = this.imageTag,
      imageSHA256 = this.imageSHA256,
      modelName = this.modelName,
      modelVersion = this.modelVersion,
      modelContract = this.modelContract,
      created = LocalDateTime.now(),
      model = Some(model),
      modelType = ModelType.fromTag(this.modelType)
    )
  }
}

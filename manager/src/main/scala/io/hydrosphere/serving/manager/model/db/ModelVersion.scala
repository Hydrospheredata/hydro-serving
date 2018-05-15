package io.hydrosphere.serving.manager.model.db

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.api.ModelType

case class ModelVersion(
  id: Long,
  imageName: String,
  imageTag: String,
  imageSHA256: String,
  created: LocalDateTime,
  modelName: String,
  modelVersion: Long,
  modelType: ModelType,
  model: Option[Model],
  modelContract: ModelContract
) {
  def toImageDef: String = imageName + ":" + imageTag
  def fullName: String = modelName + ":" + modelVersion
}

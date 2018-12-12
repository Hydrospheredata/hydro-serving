package io.hydrosphere.serving.manager.domain.clouddriver

import io.hydrosphere.serving.model.api.ModelType

case class ModelInstanceInfo(
  modelType: ModelType,
  versionId: Long,
  modelName: String,
  modelVersion: Long,
  imageName: String,
  imageTag: String
)

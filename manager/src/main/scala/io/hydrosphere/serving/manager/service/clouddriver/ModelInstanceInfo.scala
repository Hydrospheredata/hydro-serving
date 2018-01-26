package io.hydrosphere.serving.manager.service.clouddriver

import io.hydrosphere.serving.manager.service.contract.ModelType

case class ModelInstanceInfo(
  modelType: ModelType,
  modelId: Long,
  modelName: String,
  modelVersion: Long,
  imageName: String,
  imageTag: String
)

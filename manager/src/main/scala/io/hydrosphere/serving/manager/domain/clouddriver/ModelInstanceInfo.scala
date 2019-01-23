package io.hydrosphere.serving.manager.domain.clouddriver

case class ModelInstanceInfo(
  versionId: Long,
  modelName: String,
  modelVersion: Long,
  imageName: String,
  imageTag: String
)

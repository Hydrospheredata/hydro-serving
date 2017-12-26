package io.hydrosphere.serving.manager.controller

case class BuildModelRequest(
  modelId: Long,
  runtimeTypeId: Long,
  modelVersion: Option[Long],
  environmentId:Option[Long]
)

case class BuildModelByNameRequest(
  modelName: String,
  runtimeTypeId: Long,
  modelVersion: Option[Long],
  environmentId:Option[Long]
)
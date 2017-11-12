package io.hydrosphere.serving.manager.controller

case class BuildModelRequest(
  modelId: Long,
  modelVersion: Option[String],
  environmentId:Option[Long]
)

case class BuildModelByNameRequest(
  modelName: String,
  modelVersion: Option[String],
  environmentId:Option[Long]
)
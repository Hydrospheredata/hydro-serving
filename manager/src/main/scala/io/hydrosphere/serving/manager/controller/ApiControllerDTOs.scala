package io.hydrosphere.serving.manager.controller

case class BuildModelRequest(
  modelId: Long,
  modelVersion: Option[String]
)

case class BuildModelByNameRequest(
  modelName: String,
  modelVersion: Option[String]
)
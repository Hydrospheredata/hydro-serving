package io.hydrosphere.serving.manager.controller.model

case class BuildModelRequest(
  modelId: Long,
  modelVersion: Option[Long]
)

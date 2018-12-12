package io.hydrosphere.serving.manager.domain.service

import io.hydrosphere.serving.manager.domain.model_version.ModelVersion

case class Service(
  id: Long,
  serviceName: String,
  cloudDriverId: Option[String],
  modelVersion: ModelVersion,
  statusText: String,
  configParams: Map[String, String]
)
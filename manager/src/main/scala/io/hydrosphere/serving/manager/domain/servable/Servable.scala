package io.hydrosphere.serving.manager.domain.servable

import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.domain.servable.Servable.ConfigParams

case class Servable(
  id: Long,
  serviceName: String,
  cloudDriverId: Option[String],
  modelVersion: ModelVersion,
  statusText: String,
  configParams: ConfigParams
)

object Servable {
  type ConfigParams = Map[String, String]
}
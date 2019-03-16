package io.hydrosphere.serving.manager.domain.servable

import io.hydrosphere.serving.manager.domain.model_version.ModelVersion

case class ServableData(
  id: Long,
  serviceName: String,
  modelVersion: ModelVersion
)

sealed trait ServableStatus
object ServableStatus {
  case object Starting extends ServableStatus
  final case class Running(host: String, port: Int) extends ServableStatus
  case object Stopped extends ServableStatus
}

case class Servable(
  id: Long,
  modelVersionId: Long,
  serviceName: String,
  status: ServableStatus
)


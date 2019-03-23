package io.hydrosphere.serving.manager.domain.servable

sealed trait ServableStatus
object ServableStatus {
  case object Starting extends ServableStatus
  final case class Running(host: String, port: Int) extends ServableStatus
  case object Stopped extends ServableStatus
}

case class Servable(
  modelVersionId: Long,
  serviceName: String,
  status: ServableStatus
)


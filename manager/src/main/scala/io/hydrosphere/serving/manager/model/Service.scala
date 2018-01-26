package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class Service(
  id: Long,
  serviceName: String,
  cloudDriverId: Option[String],
  runtime: Runtime,
  model: Option[ModelVersion],
  environment: Option[Environment],
  statusText: String,
  configParams: Map[String, String]
)

object Service {
  implicit val serviceFormat = jsonFormat8(Service.apply)
}

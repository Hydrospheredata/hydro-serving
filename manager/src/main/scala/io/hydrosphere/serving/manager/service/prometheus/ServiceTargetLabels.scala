package io.hydrosphere.serving.manager.service.prometheus

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class ServiceTargetLabels(
  job: String,
  modelName: String,
  modelVersion: Long,
  serviceId: String,
  instanceId: String,
  serviceName: String
)

object ServiceTargetLabels {
  implicit val serviceTargetLabels = jsonFormat6(ServiceTargetLabels.apply)
}
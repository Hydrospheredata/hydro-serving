package io.hydrosphere.serving.manager.service.prometheus

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class ServiceTargets(
  targets: List[String],
  labels: ServiceTargetLabels
)

object ServiceTargets {
  implicit val serviceTargets = jsonFormat2(ServiceTargets.apply)
}
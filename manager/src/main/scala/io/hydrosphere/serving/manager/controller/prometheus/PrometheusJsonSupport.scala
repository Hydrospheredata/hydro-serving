package io.hydrosphere.serving.manager.controller.prometheus

import io.hydrosphere.serving.manager.model.ManagerJsonSupport
import io.hydrosphere.serving.manager.service.prometheus._

/**
  *
  */
trait PrometheusJsonSupport extends ManagerJsonSupport {
  implicit val serviceTargetLabels = jsonFormat6(ServiceTargetLabels)
  implicit val serviceTargets = jsonFormat2(ServiceTargets)

}

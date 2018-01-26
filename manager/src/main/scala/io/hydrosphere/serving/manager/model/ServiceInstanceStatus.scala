package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.EnumJsonConverter

object ServiceInstanceStatus extends Enumeration {
  type ServiceInstanceStatus = Value
  val DOWN, UP = Value

  implicit val modelServiceInstanceStatusFormat = new EnumJsonConverter(ServiceInstanceStatus)
}
package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class ServiceWeight(
  serviceId: Long,
  weight: Int
)
object ServiceWeight {
  implicit val serviceWeightFormat = jsonFormat2(ServiceWeight.apply)
}
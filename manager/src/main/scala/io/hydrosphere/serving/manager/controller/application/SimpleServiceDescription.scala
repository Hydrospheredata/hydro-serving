package io.hydrosphere.serving.manager.controller.application

import io.hydrosphere.serving.manager.model.{ServiceKeyDescription, WeightedService}

case class SimpleServiceDescription(
  runtimeId: Long,
  modelVersionId: Option[Long],
  environmentId: Option[Long],
  weight: Int,
  signatureName: String
) {
  def toDescription: ServiceKeyDescription = {
    ServiceKeyDescription(
      runtimeId,
      modelVersionId,
      environmentId
    )
  }

  def toWeighedService = {
    WeightedService(
      toDescription,
      weight,
      None
    )
  }
}

object SimpleServiceDescription {

  import io.hydrosphere.serving.manager.model.CommonJsonSupport._

  implicit val format = jsonFormat5(SimpleServiceDescription.apply)
}
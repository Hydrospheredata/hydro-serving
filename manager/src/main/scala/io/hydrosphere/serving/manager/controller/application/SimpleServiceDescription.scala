package io.hydrosphere.serving.manager.controller.application

import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
import io.hydrosphere.serving.manager.model.db.{ServiceKeyDescription, WeightedService}

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
  implicit val format = jsonFormat5(SimpleServiceDescription.apply)
}
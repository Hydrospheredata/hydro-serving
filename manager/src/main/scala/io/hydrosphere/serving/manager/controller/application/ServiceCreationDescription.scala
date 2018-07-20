package io.hydrosphere.serving.manager.controller.application

import io.hydrosphere.serving.manager.model.db.ServiceKeyDescription
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._

case class ServiceCreationDescription(
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
}

object ServiceCreationDescription {
  implicit val format = jsonFormat5(ServiceCreationDescription.apply)
}
package io.hydrosphere.serving.manager.api.http.controller.application

import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._

case class ServiceCreationDescription(
  modelVersionId: Long,
  weight: Int,
  signatureName: String
)

object ServiceCreationDescription {
  implicit val format = jsonFormat3(ServiceCreationDescription.apply)
}
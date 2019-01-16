package io.hydrosphere.serving.manager.domain.application

import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._

case class ModelVariantRequest(
  modelVersionId: Long,
  weight: Int,
  signatureName: String
)

object ModelVariantRequest {
  implicit val format = jsonFormat3(ModelVariantRequest.apply)
}
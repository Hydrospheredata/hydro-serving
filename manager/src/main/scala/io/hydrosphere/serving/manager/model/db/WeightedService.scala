package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.contract.model_signature.ModelSignature

case class WeightedService(
  serviceDescription: ServiceKeyDescription,
  weight: Int,
  signature: Option[ModelSignature]
)

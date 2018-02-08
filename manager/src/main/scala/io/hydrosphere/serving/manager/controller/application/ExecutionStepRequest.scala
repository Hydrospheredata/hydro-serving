package io.hydrosphere.serving.manager.controller.application

import io.hydrosphere.serving.manager.model.WeightedService

case class ExecutionStepRequest(
  services: List[WeightedService],
  signatureName: String
)

object ExecutionStepRequest {
  import io.hydrosphere.serving.manager.model.CommonJsonSupport._
  implicit val format = jsonFormat2(ExecutionStepRequest.apply)
}
package io.hydrosphere.serving.manager.controller.application

case class ExecutionStepRequest(
  services: List[SimpleServiceDescription]
)

object ExecutionStepRequest {
  import io.hydrosphere.serving.manager.model.CommonJsonSupport._
  implicit val format = jsonFormat1(ExecutionStepRequest.apply)
}
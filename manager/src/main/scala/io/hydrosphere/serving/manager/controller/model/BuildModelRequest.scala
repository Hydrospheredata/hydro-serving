package io.hydrosphere.serving.manager.controller.model

case class BuildModelRequest(
  modelId: Long
)

object BuildModelRequest {
  import io.hydrosphere.serving.manager.model.CommonJsonSupport._

  implicit val buildModelRequestFormat = jsonFormat1(BuildModelRequest.apply)
}
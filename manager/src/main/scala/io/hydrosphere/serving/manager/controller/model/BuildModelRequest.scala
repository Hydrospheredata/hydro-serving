package io.hydrosphere.serving.manager.controller.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class BuildModelRequest(
  modelId: Long,
  modelVersion: Option[Long]
)

object BuildModelRequest {
  implicit val buildModelRequestFormat = jsonFormat2(BuildModelRequest.apply)
}
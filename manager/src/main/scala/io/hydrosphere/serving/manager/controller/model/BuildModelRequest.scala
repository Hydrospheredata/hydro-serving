package io.hydrosphere.serving.manager.controller.model

import io.hydrosphere.serving.manager.model.api.description.ContractDescription

case class BuildModelRequest(
  modelId: Long,
  flatContract:Option[ContractDescription]
)

object BuildModelRequest {
  import io.hydrosphere.serving.manager.model.CommonJsonSupport._

  implicit val buildModelRequestFormat = jsonFormat2(BuildModelRequest.apply)
}
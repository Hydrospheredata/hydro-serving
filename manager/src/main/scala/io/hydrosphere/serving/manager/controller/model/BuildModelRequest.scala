package io.hydrosphere.serving.manager.controller.model

import io.hydrosphere.serving.contract.utils.description.ContractDescription

case class BuildModelRequest(
  modelId: Long,
  flatContract:Option[ContractDescription]
)

object BuildModelRequest {
  import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._

  implicit val buildModelRequestFormat = jsonFormat2(BuildModelRequest.apply)
}
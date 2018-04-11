package io.hydrosphere.serving.manager.controller.model_source

case class AddLocalSourceRequest(
  name: String,
  path: String
)

object AddLocalSourceRequest {
  import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._

  implicit val format = jsonFormat2(AddLocalSourceRequest.apply)
}
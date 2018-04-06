package io.hydrosphere.serving.manager.controller.model_source

import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.AWSAuthKeys

case class AddS3SourceRequest(
  name: String,
  key: Option[AWSAuthKeys],
  bucket: String,
  path: String,
  region: String
)

object AddS3SourceRequest {
  import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._

  implicit val format = jsonFormat5(AddS3SourceRequest.apply)
}
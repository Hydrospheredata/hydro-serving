package io.hydrosphere.serving.manager.service.management.source

import io.hydrosphere.serving.manager.util.CommonJsonSupport._
import io.hydrosphere.serving.manager.model.SourceParams

case class CreateModelSourceRequest(
  name: String,
  params: SourceParams
)

object CreateModelSourceRequest {
  implicit val createModelSourceRequestFormat = jsonFormat2(CreateModelSourceRequest.apply)
}

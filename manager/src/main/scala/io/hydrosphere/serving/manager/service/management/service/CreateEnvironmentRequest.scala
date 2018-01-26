package io.hydrosphere.serving.manager.service.management.service

import io.hydrosphere.serving.manager.util.CommonJsonSupport._
import io.hydrosphere.serving.manager.model.Environment

case class CreateEnvironmentRequest(
  name: String,
  placeholders: Seq[Any]
) {
  def toEnvironment: Environment = {
    Environment(
      name = this.name,
      placeholders = this.placeholders,
      id = 0
    )
  }
}

object CreateEnvironmentRequest {
  implicit val createEnvironmentRequest = jsonFormat2(CreateEnvironmentRequest.apply)
}
package io.hydrosphere.serving.manager.service.management.runtime

import io.hydrosphere.serving.manager.util.CommonJsonSupport._
import io.hydrosphere.serving.manager.model.Runtime
import io.hydrosphere.serving.manager.service.contract.ModelType

case class CreateRuntimeRequest(
  name: String,
  version: String,
  modelTypes: Option[List[String]] = None,
  tags: Option[List[String]] = None,
  configParams: Option[Map[String, String]] = None
) {
  def toRuntimeType: Runtime = {
    Runtime(
      id = 0,
      name = this.name,
      version = this.version,
      suitableModelType = this.tags
        .getOrElse(List())
        .map(ModelType.fromTag),
      tags = this.tags.getOrElse(List()),
      configParams = this.configParams.getOrElse(Map())
    )
  }
}

object CreateRuntimeRequest {
  implicit val createRuntimeRequest = jsonFormat5(CreateRuntimeRequest.apply)
}

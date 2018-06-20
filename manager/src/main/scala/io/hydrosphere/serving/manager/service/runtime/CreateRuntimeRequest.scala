package io.hydrosphere.serving.manager.service.runtime

import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.service.ServiceRequest

case class CreateRuntimeRequest(
  name: String,
  version: String,
  modelTypes: List[ModelType] = List.empty,
  tags: List[String] = List.empty,
  configParams: Map[String, String] = Map.empty
) extends ServiceRequest {
  def fullImage = name + ":" + version
}

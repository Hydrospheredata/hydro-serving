package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.manager.model.api.ModelType

case class Runtime(
  id: Long,
  name: String,
  version: String,
  suitableModelType: List[ModelType],
  tags: List[String],
  configParams: Map[String, String]
) {
  def toImageDef: String = s"$name:$version"
}

package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.manager.grpc.entities.{Runtime => GRuntime}

case class Runtime(
  id: Long,
  name: String,
  version: String,
  suitableModelType: List[ModelType],
  tags: List[String],
  configParams: Map[String, String]
) {
  def toImageDef: String = name + ":" + version

  def toGrpc = GRuntime(
    id = id,
    name = name,
    version = version
  )
}

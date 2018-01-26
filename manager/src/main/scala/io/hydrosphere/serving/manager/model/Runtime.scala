package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._
import io.hydrosphere.serving.manager.service.contract.ModelType

case class Runtime(
  id: Long,
  name: String,
  version: String,
  suitableModelType: List[ModelType],
  tags: List[String],
  configParams: Map[String, String]
){
  def toImageDef: String = s"$name:$version"
}

object Runtime {
  implicit val runtimeFormat = jsonFormat6(Runtime.apply)

  class Schematic(
    name: String,
    version: String,
    suitableModelType: List[ModelType]
  ) extends Runtime(-1, name, version, suitableModelType, List(), Map())
}

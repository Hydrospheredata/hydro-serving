package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._
import spray.json.JsonFormat

case class ModelSourceConfigAux(
  id: Long,
  name: String,
  params: SourceParams
){
  def toTyped[T <: SourceParams]: ModelSourceConfig[T] = ModelSourceConfig(id, name, params.asInstanceOf[T])
}

object ModelSourceConfigAux {
  implicit val modelSourceConfigFormat: JsonFormat[ModelSourceConfigAux] = jsonFormat3(ModelSourceConfigAux.apply)
}

case class ModelSourceConfig[T <: SourceParams](
  id: Long,
  name: String,
  params: T
) {
  def toAux: ModelSourceConfigAux = {
    ModelSourceConfigAux(id, name, params)
  }
}

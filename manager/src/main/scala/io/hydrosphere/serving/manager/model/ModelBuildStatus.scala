package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.EnumJsonConverter

object ModelBuildStatus extends Enumeration {
  type ModelBuildStatus = Value
  val STARTED, FINISHED, ERROR = Value

  implicit val modelBuildStatusFormat = new EnumJsonConverter(ModelBuildStatus)
}

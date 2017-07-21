package io.hydrosphere.serving.manager.controller

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.model.{CommonJsonSupport, EnumJsonConverter}

/**
  *
  */
trait ManagerJsonSupport extends CommonJsonSupport {
  implicit val modelBuildStatusFormat = new EnumJsonConverter(ModelBuildStatus)

  implicit val runtimeTypeFormat = jsonFormat3(RuntimeType)

  implicit val modelFormat = jsonFormat9(Model)

  implicit val buildModelRequestFormat = jsonFormat2(BuildModelRequest)

  implicit val modelBuildFormat = jsonFormat7(ModelBuild)

}

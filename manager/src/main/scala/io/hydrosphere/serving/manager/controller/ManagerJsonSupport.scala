package io.hydrosphere.serving.manager.controller

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.model.{CommonJsonSupport, EnumJsonConverter}

/**
  *
  */
trait ManagerJsonSupport extends CommonJsonSupport {
  implicit val modelBuildStatusFormat = new EnumJsonConverter(ModelBuildStatus)
  implicit val modelServiceInstanceStatusFormat = new EnumJsonConverter(ModelServiceInstanceStatus)

  implicit val runtimeTypeFormat = jsonFormat3(RuntimeType)

  implicit val modelFormat = jsonFormat9(Model)

  implicit val buildModelRequestFormat = jsonFormat2(BuildModelRequest)

  implicit val modelRuntimeFormat = jsonFormat12(ModelRuntime)

  implicit val modelBuildFormat = jsonFormat9(ModelBuild)

  implicit val modelServiceFormat = jsonFormat6(ModelService)

  implicit val modelServiceInstanceFormat = jsonFormat7(ModelServiceInstance)

}

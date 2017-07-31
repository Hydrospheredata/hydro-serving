package io.hydrosphere.serving.manager.controller

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.model._

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

  implicit val createEndpointRequest = jsonFormat2(CreateEndpointRequest)

  implicit val createModelServiceRequest = jsonFormat2(CreateModelServiceRequest)

  implicit val createRuntimeTypeRequest = jsonFormat2(CreateRuntimeTypeRequest)

  implicit val createOrUpdateModelRequest = jsonFormat7(CreateOrUpdateModelRequest)

  implicit val createModelRuntime = jsonFormat10(CreateModelRuntime)

  implicit val updateModelRuntime = jsonFormat7(UpdateModelRuntime)

  implicit val createPipelineStageRequest = jsonFormat2(CreatePipelineStageRequest)

  implicit val createPipelineRequest = jsonFormat2(CreatePipelineRequest)
}

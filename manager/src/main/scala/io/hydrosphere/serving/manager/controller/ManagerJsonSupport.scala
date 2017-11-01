package io.hydrosphere.serving.manager.controller

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service.{WeightedServiceCreateOrUpdateRequest, _}
import io.hydrosphere.serving.model._

/**
  *
  */
trait ManagerJsonSupport extends CommonJsonSupport {
  implicit val modelBuildStatusFormat = new EnumJsonConverter(ModelBuildStatus)
  implicit val modelServiceInstanceStatusFormat = new EnumJsonConverter(ModelServiceInstanceStatus)

  implicit val modelFormat = jsonFormat9(Model)

  implicit val buildModelRequestFormat = jsonFormat2(BuildModelRequest)
  implicit val buildModelByNameRequest = jsonFormat2(BuildModelByNameRequest)

  implicit val modelBuildFormat = jsonFormat9(ModelBuild)

  implicit val modelServiceInstanceFormat = jsonFormat8(ModelServiceInstance)

  implicit val createEndpointRequest = jsonFormat2(CreateEndpointRequest)

  implicit val createModelServiceRequest = jsonFormat3(CreateModelServiceRequest)

  implicit val createRuntimeTypeRequest = jsonFormat4(CreateRuntimeTypeRequest)

  implicit val createOrUpdateModelRequest = jsonFormat7(CreateOrUpdateModelRequest)

  implicit val createModelRuntime = jsonFormat12(CreateModelRuntime)

  implicit val updateModelRuntime = jsonFormat7(UpdateModelRuntime)

  implicit val createPipelineStageRequest = jsonFormat2(CreatePipelineStageRequest)

  implicit val createPipelineRequest = jsonFormat2(CreatePipelineRequest)

  implicit val weightedServiceCreateOrUpdateRequest = jsonFormat4(WeightedServiceCreateOrUpdateRequest)

}

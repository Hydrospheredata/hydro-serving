package io.hydrosphere.serving.manager.service.management.service

import io.hydrosphere.serving.manager.model.{Environment, ModelVersion, Service, Runtime}

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class CreateServiceRequest(
  serviceName: String,
  runtimeId: Long,
  configParams: Option[Map[String, String]],
  environmentId: Option[Long],
  modelVersionId: Option[Long]
) {
  def toService(runtime: Runtime, model: Option[ModelVersion], environment: Option[Environment]): Service = {
    Service(
      id = 0,
      serviceName = this.serviceName,
      cloudDriverId = None,
      runtime = runtime,
      model = model,
      statusText = "New",
      environment = environment,
      configParams = runtime.configParams ++ this.configParams.getOrElse(Map())
    )
  }
}

object CreateServiceRequest {
  implicit val createServiceRequestFormat = jsonFormat5(CreateServiceRequest.apply)
}
package io.hydrosphere.serving.manager.service.service

import io.hydrosphere.serving.manager.model.db.{Runtime, Environment, ModelVersion, Service}

case class CreateServiceRequest(
  serviceName: String,
  runtimeId: Long,
  configParams: Option[Map[String, String]],
  environmentId: Option[Long],
  modelVersionId: Option[Long]
) {
  def toService(runtime: Runtime, model: Option[ModelVersion], environment: Option[Environment]): Service =
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

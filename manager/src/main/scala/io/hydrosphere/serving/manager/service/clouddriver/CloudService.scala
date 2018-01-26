package io.hydrosphere.serving.manager.service.clouddriver

case class CloudService(
  id: Long,
  serviceName: String,
  statusText: String,
  cloudDriverId: String,
  environmentName: Option[String],
  configParams: Map[String, String],
  runtimeInfo: MainApplicationInstanceInfo,
  modelInfo: Option[ModelInstanceInfo],
  instances: Seq[ServiceInstance]
)

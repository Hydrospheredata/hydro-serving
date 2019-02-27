package io.hydrosphere.serving.manager.domain.clouddriver

case class CloudService(
  id: Long,
  serviceName: String,
  statusText: String,
  cloudDriverId: String,
  instances: Seq[ServiceInstance]
)

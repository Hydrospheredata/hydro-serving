package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.model.ServiceInstanceStatus.ServiceInstanceStatus

case class ServiceInstance(
  instanceId: String,
  host: String,
  appPort: Int,
  sidecarPort: Int,
  sidecarAdminPort: Int,
  serviceId: Long,
  status: ServiceInstanceStatus,
  statusText: Option[String]
)
package io.hydrosphere.serving.manager.domain.clouddriver

case class ServiceInstance(
  instanceId: String,
  mainApplication: MainApplicationInstance,
  sidecar: SidecarInstance,
  model: Option[ModelInstance],
  advertisedHost: String,
  advertisedPort: Int
)

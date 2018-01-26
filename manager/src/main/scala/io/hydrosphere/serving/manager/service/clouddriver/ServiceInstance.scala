package io.hydrosphere.serving.manager.service.clouddriver

case class ServiceInstance(
  instanceId: String,
  mainApplication: MainApplicationInstance,
  sidecar: SidecarInstance,
  model: Option[ModelInstance]
)

package io.hydrosphere.serving.manager.domain.clouddriver

case class ServiceInstance(
  instanceId: String,
  mainApplication: MainApplicationInstance,
  model: Option[ModelInstance],
  advertisedHost: String,
  advertisedPort: Int
)

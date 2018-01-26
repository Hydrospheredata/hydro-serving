package io.hydrosphere.serving.manager.service.clouddriver

case class SidecarInstance(
  instanceId: String,
  host: String,
  ingressPort: Int,
  egressPort: Int,
  adminPort: Int
)

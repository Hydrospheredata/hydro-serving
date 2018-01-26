package io.hydrosphere.serving.manager.configuration


case class SidecarConfig(
  host: String,
  ingressPort: Int,
  egressPort: Int,
  adminPort: Int
)

package io.hydrosphere.serving.manager.config

case class SidecarConfig(
  host: String,
  ingressPort: Int,
  egressPort: Int,
  adminPort: Int
)

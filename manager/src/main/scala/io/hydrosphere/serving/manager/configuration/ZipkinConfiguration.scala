package io.hydrosphere.serving.manager.configuration


case class ZipkinConfiguration(
  host: String,
  port: Int,
  enabled: Boolean
)

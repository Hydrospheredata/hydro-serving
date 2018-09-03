package io.hydrosphere.serving.manager.config

case class OpenTracingConfiguration(zipkin: ZipkinConfiguration)

case class ZipkinConfiguration(
  host: String,
  port: Int,
  enabled: Boolean
)
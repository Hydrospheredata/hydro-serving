package io.hydrosphere.serving.manager.config

case class InfluxDBMetricsConfiguration(
  collectTimeout: Int,
  port: Int,
  host: String,
  databaseName: String
)

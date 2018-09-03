package io.hydrosphere.serving.manager.config

case class ModelLoggingConfiguration(
  driver: String,
  params: Map[String, String]
)

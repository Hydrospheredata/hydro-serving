package io.hydrosphere.serving.manager.domain.clouddriver

case class MainApplicationInstance(
  instanceId: String,
  host: String,
  port: Int
)

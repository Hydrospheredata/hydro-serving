package io.hydrosphere.serving.manager.controller.application

case class AddApplicationSourceRequest(
  runtimeId: Long,
  configParams: Option[Map[String, String]]
)

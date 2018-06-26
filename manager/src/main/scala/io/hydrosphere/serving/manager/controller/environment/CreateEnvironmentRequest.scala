package io.hydrosphere.serving.manager.controller.environment

case class CreateEnvironmentRequest(
  name: String,
  placeholders: Seq[Any]
)
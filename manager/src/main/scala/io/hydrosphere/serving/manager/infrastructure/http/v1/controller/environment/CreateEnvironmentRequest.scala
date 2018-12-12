package io.hydrosphere.serving.manager.infrastructure.http.v1.controller.environment

case class CreateEnvironmentRequest(
  name: String,
  placeholders: Seq[Any]
)
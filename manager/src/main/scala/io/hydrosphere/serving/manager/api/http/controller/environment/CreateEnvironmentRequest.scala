package io.hydrosphere.serving.manager.api.http.controller.environment

case class CreateEnvironmentRequest(
  name: String,
  placeholders: String
)
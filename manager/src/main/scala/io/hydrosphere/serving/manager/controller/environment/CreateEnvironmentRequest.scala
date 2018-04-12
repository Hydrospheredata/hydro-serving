package io.hydrosphere.serving.manager.controller.environment

import io.hydrosphere.serving.manager.model.db.Environment

case class CreateEnvironmentRequest(
  name: String,
  placeholders: Seq[Any]
)
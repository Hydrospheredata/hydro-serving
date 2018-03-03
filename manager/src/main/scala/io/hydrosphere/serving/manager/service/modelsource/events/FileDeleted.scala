package io.hydrosphere.serving.manager.service.modelsource.events

import java.time.Instant

import io.hydrosphere.serving.manager.service.modelsource.ModelSource

case class FileDeleted(
  source: ModelSource,
  filename: String,
  timestamp: Instant
) extends FileEvent

package io.hydrosphere.serving.manager.service.modelsource.events

import java.time.Instant

import io.hydrosphere.serving.manager.service.modelsource.ModelSource

case class FileDetected(
  source: ModelSource,
  filename: String,
  timestamp: Instant,
  hash: String
) extends FileEvent

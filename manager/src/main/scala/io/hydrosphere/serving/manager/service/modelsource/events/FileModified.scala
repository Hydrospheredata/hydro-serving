package io.hydrosphere.serving.manager.service.modelsource.events

import java.time.{Instant, LocalDateTime}

import io.hydrosphere.serving.manager.service.modelsource.ModelSource

case class FileModified(
  source: ModelSource,
  filename: String,
  timestamp: Instant,
  hash: String,
  updatedAt: LocalDateTime
) extends FileEvent

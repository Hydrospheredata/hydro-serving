package io.hydrosphere.serving.manager.service.modelsource

import java.time.{Instant, LocalDateTime}

trait WatcherEvent {
  def source: ModelSource
  def timestamp: Instant
}

trait FileEvent extends WatcherEvent {
  def filename: String
}

case class FileDetected(
  source: ModelSource,
  filename: String,
  timestamp: Instant,
  hash: String
) extends FileEvent

case class FileDeleted(
  source: ModelSource,
  filename: String,
  timestamp: Instant
) extends FileEvent

case class FileCreated(
  source: ModelSource,
  filename: String,
  timestamp: Instant,
  hash: String,
  createdAt: LocalDateTime
) extends FileEvent

case class FileModified(
  source: ModelSource,
  filename: String,
  timestamp: Instant,
  hash: String,
  updatedAt: LocalDateTime
) extends FileEvent
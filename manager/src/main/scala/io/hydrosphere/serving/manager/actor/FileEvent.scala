package io.hydrosphere.serving.manager.actor

import java.time.{Instant, LocalDateTime}

import io.hydrosphere.serving.manager.service.modelsource.ModelSource

abstract class FileEvent(val source: ModelSource, val filename: String, val timestamp: Instant)

class FileDetected(source: ModelSource, filename: String, timestamp: Instant, val hash: String)
  extends FileEvent(source, filename, timestamp)

class FileDeleted(source: ModelSource, filename: String, timestamp: Instant)
  extends FileEvent(source, filename, timestamp)

class FileCreated(source: ModelSource, filename: String, timestamp: Instant, val hash: String, val createdAt: LocalDateTime)
  extends FileEvent(source, filename, timestamp)

class FileModified(source: ModelSource, filename: String, timestamp: Instant, val hash: String, val updatedAt: LocalDateTime)
  extends FileEvent(source, filename, timestamp)
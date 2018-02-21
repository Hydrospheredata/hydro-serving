package io.hydrosphere.serving.manager.service.modelsource.events

import java.time.Instant

import io.hydrosphere.serving.manager.service.modelsource.ModelSource

trait WatcherEvent {
  def source: ModelSource
  def timestamp: Instant
}

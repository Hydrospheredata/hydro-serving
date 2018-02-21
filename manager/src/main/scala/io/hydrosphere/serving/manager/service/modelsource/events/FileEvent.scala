package io.hydrosphere.serving.manager.service.modelsource.events

trait FileEvent extends WatcherEvent {
  def filename: String
}

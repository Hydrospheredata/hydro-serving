package io.hydrosphere.serving.manager.discovery

import io.hydrosphere.serving.manager.domain.application.Application

sealed trait DiscoveryEvent
object DiscoveryEvent {
  final case class AppRemoved(id: Long) extends DiscoveryEvent
  final case class AppStarted(app: Application) extends DiscoveryEvent
}


package io.hydrosphere.serving.manager.discovery

import io.hydrosphere.serving.discovery.serving.ServingApp

sealed trait DiscoveryEvent
object DiscoveryEvent {
  final case class AppRemoved(id: Long) extends DiscoveryEvent
  final case class AppStarted(app: ServingApp) extends DiscoveryEvent
}


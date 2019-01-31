package io.hydrosphere.serving.manager.infrastructure.envoy.events

import io.hydrosphere.serving.manager.domain.application.Application
import io.hydrosphere.serving.manager.domain.clouddriver.CloudService
import io.hydrosphere.serving.manager.domain.servable.Servable

sealed trait DiscoveryEvent extends Product with Serializable

object DiscoveryEvent {

  final case class ApplicationChanged(application: Application) extends DiscoveryEvent

  case class ApplicationRemoved(application: Application) extends DiscoveryEvent

  case class ServiceChanged(service: Servable) extends DiscoveryEvent

  case class ServiceRemoved(service: Servable) extends DiscoveryEvent

  case class CloudServiceDetected(cloudServices: Seq[CloudService]) extends DiscoveryEvent

  case class CloudServiceRemoved(cloudServices: Seq[CloudService]) extends DiscoveryEvent

}

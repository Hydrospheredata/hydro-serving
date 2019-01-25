package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.domain.application.Application
import io.hydrosphere.serving.manager.domain.servable.Servable
import io.hydrosphere.serving.manager.domain.clouddriver.CloudService

package object internal_events {
  case class ApplicationChanged(application: Application)

  case class ApplicationRemoved(application: Application)

  case class ServiceChanged(service: Servable)

  case class ServiceRemoved(service: Servable)

  case class CloudServiceDetected(cloudServices: Seq[CloudService])

  case class CloudServiceRemoved(cloudServices: Seq[CloudService])
}

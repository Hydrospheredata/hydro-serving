package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.domain.application.Application
import io.hydrosphere.serving.manager.domain.service.Service
import io.hydrosphere.serving.manager.domain.clouddriver.CloudService

package object internal_events {
  case class ApplicationChanged(application: Application)

  case class ApplicationRemoved(application: Application)

  case class ServiceChanged(service: Service)

  case class ServiceRemoved(service: Service)

  case class CloudServiceDetected(cloudServices: Seq[CloudService])

  case class CloudServiceRemoved(cloudServices: Seq[CloudService])
}

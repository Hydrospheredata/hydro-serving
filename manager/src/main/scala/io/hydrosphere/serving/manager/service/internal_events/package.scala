package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.model.db.{Application, Service}
import io.hydrosphere.serving.manager.service.clouddriver.CloudService

package object internal_events {
  case class ApplicationChanged(application: Application)

  case class ApplicationRemoved(application: Application)

  case class ServiceChanged(service: Service)

  case class ServiceRemoved(service: Service)

  case class CloudServiceDetected(cloudServices: Seq[CloudService])
}

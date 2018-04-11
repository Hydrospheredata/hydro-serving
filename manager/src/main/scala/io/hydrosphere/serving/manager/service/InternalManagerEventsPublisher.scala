package io.hydrosphere.serving.manager.service

import akka.actor.ActorSystem
import io.hydrosphere.serving.manager.model.db.{Application, Service}
import io.hydrosphere.serving.manager.service.clouddriver.CloudService

case class ApplicationChanged(application: Application)

case class ApplicationRemoved(application: Application)

case class ServiceChanged(service: Service)

case class ServiceRemoved(service: Service)

case class CloudServiceDetected(cloudServices: Seq[CloudService])

class InternalManagerEventsPublisher(implicit actorSystem: ActorSystem) {

  def applicationChanged(application: Application): Unit =
    actorSystem.eventStream.publish(ApplicationChanged(application))

  def applicationRemoved(application: Application): Unit =
    actorSystem.eventStream.publish(ApplicationRemoved(application))

  def serviceChanged(service: Service): Unit =
    actorSystem.eventStream.publish(ServiceChanged(service))

  def serviceRemoved(service: Service): Unit =
    actorSystem.eventStream.publish(ServiceRemoved(service))

  def cloudServiceDetected(cloudService: Seq[CloudService]): Unit =
    actorSystem.eventStream.publish(CloudServiceDetected(cloudService))
}



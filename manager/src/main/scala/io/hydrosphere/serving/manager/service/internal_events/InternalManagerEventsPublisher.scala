package io.hydrosphere.serving.manager.service.internal_events

import akka.actor.ActorSystem
import io.hydrosphere.serving.manager.model.db.{Application, Service}
import io.hydrosphere.serving.manager.service.clouddriver.CloudService
import org.apache.logging.log4j.scala.Logging

class InternalManagerEventsPublisher(implicit actorSystem: ActorSystem) extends Logging {

  def applicationChanged(application: Application): Unit = {
    logger.info(s"Application changed: $application")
    actorSystem.eventStream.publish(ApplicationChanged(application))
  }

  def applicationRemoved(application: Application): Unit = {
    logger.info(s"Application removed: $application")
    actorSystem.eventStream.publish(ApplicationRemoved(application))
  }

  def serviceChanged(service: Service): Unit = {
    logger.info(s"Service changed: $service")
    actorSystem.eventStream.publish(ServiceChanged(service))
  }

  def serviceRemoved(service: Service): Unit = {
    logger.info(s"Service removed: $service")
    actorSystem.eventStream.publish(ServiceRemoved(service))
  }

  def cloudServiceDetected(cloudService: Seq[CloudService]): Unit = {
    logger.info(s"Cloud service detected: $cloudService")
    actorSystem.eventStream.publish(CloudServiceDetected(cloudService))
  }

  def cloudServiceRemoved(cloudService: Seq[CloudService]): Unit = {
    logger.info(s"Cloud service removed: $cloudService")
    actorSystem.eventStream.publish(CloudServiceRemoved(cloudService))
  }
}



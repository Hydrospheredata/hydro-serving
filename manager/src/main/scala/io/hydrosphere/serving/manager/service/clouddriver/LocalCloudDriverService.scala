package io.hydrosphere.serving.manager.service.clouddriver
import io.hydrosphere.serving.manager.model.{Environment, Service}

import scala.concurrent.Future

/**
  *
  */
class LocalCloudDriverService extends CloudDriverService{
  override def serviceList(): Future[Seq[CloudService]] = ???

  override def deployService(service: Service, environment: Environment): Future[CloudService] = ???

  override def services(serviceIds: Seq[Long]): Future[Seq[CloudService]] = ???

  override def removeService(serviceId: Long): Future[Unit] = ???
}

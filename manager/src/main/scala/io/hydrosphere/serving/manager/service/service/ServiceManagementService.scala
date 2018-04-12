package io.hydrosphere.serving.manager.service.service

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db._

import scala.concurrent.Future

trait ServiceManagementService {
  def deleteService(serviceId: Long): HFResult[Service]

  def addService(r: CreateServiceRequest): HFResult[Service]

  def allServices(): Future[Seq[Service]]

  def servicesByIds(ids: Seq[Long]): Future[Seq[Service]]

  def getServicesByModel(modelId: Long): Future[Seq[Service]]

  def getServicesByRuntimes(runtimeId: Set[Long]): Future[Seq[Service]]

  def getService(serviceId: Long): HFResult[Service]

  def fetchServicesUnsync(services: Set[ServiceKeyDescription]): Future[Seq[Service]]
}


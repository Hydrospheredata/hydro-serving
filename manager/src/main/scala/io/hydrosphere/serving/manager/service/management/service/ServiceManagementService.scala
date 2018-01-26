package io.hydrosphere.serving.manager.service.management.service

import io.hydrosphere.serving.manager.model._

import scala.concurrent.Future


trait ServiceManagementService {
  val MANAGER_ID: Long = -20
  val GATEWAY_ID: Long = -10
  val MANAGER_NAME: String = "manager"
  val GATEWAY_NAME: String = "gateway"

  def deleteService(serviceId: Long): Future[Unit]

  def addService(r: CreateServiceRequest): Future[Service]

  def allServices(): Future[Seq[Service]]

  def servicesByIds(ids: Seq[Long]): Future[Seq[Service]]

  def getServicesByModel(modelId: Long): Future[Seq[Service]]

  def getServicesByRuntimes(runtimeId: Seq[Long]): Future[Seq[Service]]

  def getService(serviceId: Long): Future[Option[Service]]

  def serviceByFullName(fullName: String): Future[Option[Service]]

  def allEnvironments(): Future[Seq[Environment]]

  def createEnvironment(r: CreateEnvironmentRequest): Future[Environment]

  def deleteEnvironment(environmentId: Long): Future[Unit]

  def serveService(serviceId: Long, inputData: Array[Byte]): Future[Array[Byte]]
}

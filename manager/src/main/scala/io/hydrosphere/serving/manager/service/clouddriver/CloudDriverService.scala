package io.hydrosphere.serving.manager.service.clouddriver

import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.{Environment, Service}

import scala.concurrent.Future

case class ModelInstanceInfo(
  modelType: ModelType,
  modelId: Long,
  modelName: String,
  modelVersion: Long,
  imageName: String,
  imageTag: String
)

case class ModelInstance(
  instanceId: String
)

case class SidecarInstance(
  instanceId: String,
  host: String,
  ingressPort: Int,
  egressPort: Int,
  adminPort: Int
)

case class MainApplicationInstanceInfo(
  runtimeId: Long,
  runtimeName: String,
  runtimeVersion: String
)

case class MainApplicationInstance(
  instanceId: String,
  host: String,
  port: Int
)

case class ServiceInstance(
  instanceId: String,
  mainApplication: MainApplicationInstance,
  sidecar: SidecarInstance,
  model: Option[ModelInstance]
)

case class CloudService(
  id: Long,
  serviceName: String,
  statusText: String,
  cloudDriverId: String,
  environmentName: Option[String],
  configParams: Map[String, String],
  runtimeInfo: MainApplicationInstanceInfo,
  modelInfo: Option[ModelInstanceInfo],
  instances: Seq[ServiceInstance]
)

trait CloudDriverService {

  def serviceList(): Future[Seq[CloudService]]

  def deployService(service: Service, environment: Environment): Future[CloudService]

  def services(serviceIds: Seq[Long]): Future[Seq[CloudService]]

  def removeService(serviceId: Long): Future[Unit]
}
package io.hydrosphere.serving.manager.service.clouddriver

import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.Service

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
  model: Option[ModelInstance],
  advertisedHost:String,
  advertisedPort:Int
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

  val LABEL_SERVICE_ID = "hydroServingServiceId"
  val LABEL_SERVICE_NAME = "LABEL_SERVICE_NAME"
  val LABEL_HS_SERVICE_MARKER = "HS_SERVICE_MARKER"
  val LABEL_MODEL_VERSION_ID = "MODEL_VERSION_ID"
  val LABEL_MODEL_VERSION = "MODEL_VERSION"
  val LABEL_MODEL_NAME = "MODEL_NAME"
  val LABEL_MODEL_TYPE = "MODEL_TYPE"
  val LABEL_RUNTIME_ID = "MODEL_RUNTIME_ID"

  val LABEL_DEPLOYMENT_TYPE="DEPLOYMENT_TYPE"

  val DEPLOYMENT_TYPE_MODEL="MODEL"
  val DEPLOYMENT_TYPE_APP="APP"
  val DEPLOYMENT_TYPE_SIDECAR="SIDECAR"


  val ENV_APP_PORT = "APP_PORT"
  val ENV_SIDECAR_PORT = "SIDECAR_PORT"
  val ENV_SIDECAR_HOST = "SIDECAR_HOST"

  val ENV_MODEL_DIR = "MODEL_DIR"

  val DEFAULT_MODEL_DIR = "/model"
  val DEFAULT_APP_PORT = 9091
  val DEFAULT_SIDECAR_INGRESS_PORT = 8080
  val DEFAULT_SIDECAR_EGRESS_PORT = 8081
  val DEFAULT_SIDECAR_ADMIN_PORT = 8082


  def serviceList(): Future[Seq[CloudService]]

  def deployService(service: Service): Future[CloudService]

  def services(serviceIds: Set[Long]): Future[Seq[CloudService]]

  def removeService(serviceId: Long): Future[Unit]

  protected def getModelLabels(service: Service): Map[String, String] = {
    val model = service.model.getOrElse(throw new IllegalArgumentException("ModelVersion required"))
    Map[String, String](
      LABEL_SERVICE_ID -> service.id.toString,
      LABEL_HS_SERVICE_MARKER -> LABEL_HS_SERVICE_MARKER,
      LABEL_MODEL_NAME -> model.modelName,
      LABEL_MODEL_VERSION -> model.modelVersion.toString,
      LABEL_MODEL_VERSION_ID -> model.id.toString,
      LABEL_MODEL_TYPE -> model.modelType.toTag,
      LABEL_DEPLOYMENT_TYPE -> DEPLOYMENT_TYPE_MODEL
    )
  }

  protected def getRuntimeLabels(service: Service): Map[String, String] = {
    Map[String, String](
      LABEL_SERVICE_ID -> service.id.toString,
      LABEL_HS_SERVICE_MARKER -> LABEL_HS_SERVICE_MARKER,
      LABEL_RUNTIME_ID -> service.runtime.id.toString,
      LABEL_DEPLOYMENT_TYPE -> DEPLOYMENT_TYPE_APP
    )
  }

  protected def createCloudService(service: Service, statusText: String, cloudDriverId: String, instances: Seq[ServiceInstance]): CloudService =
    CloudService(
      id = service.id,
      serviceName = service.serviceName,
      statusText = statusText,
      cloudDriverId = cloudDriverId,
      environmentName = service.environment.map(e => e.name),
      configParams = service.configParams,
      runtimeInfo = MainApplicationInstanceInfo(
        runtimeId = service.runtime.id,
        runtimeName = service.runtime.name,
        runtimeVersion = service.runtime.version
      ),
      modelInfo = service.model.map(model => {
        ModelInstanceInfo(
          modelType = model.modelType,
          modelId = model.id,
          modelName = model.modelName,
          modelVersion = model.modelVersion,
          imageName = model.imageName,
          imageTag = model.imageTag
        )
      }),
      instances = instances
    )
}
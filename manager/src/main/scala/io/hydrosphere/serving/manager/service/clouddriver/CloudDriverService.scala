package io.hydrosphere.serving.manager.service.clouddriver

import io.hydrosphere.serving.manager.model.db.Service
import io.hydrosphere.serving.model.api.ModelType
import org.apache.logging.log4j.scala.Logging

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
  advertisedHost: String,
  advertisedPort: Int
)

case class CloudService(
  id: Long,
  serviceName: String,
  statusText: String,
  cloudDriverId: String,
  environmentName: Option[String],
  runtimeInfo: MainApplicationInstanceInfo,
  modelInfo: Option[ModelInstanceInfo],
  instances: Seq[ServiceInstance]
)


case class MetricServiceTargetLabels(
  job: Option[String],
  modelName: Option[String],
  modelVersion: Option[String],
  environment: Option[String],
  runtimeName: Option[String],
  runtimeVersion: Option[String],
  serviceName: Option[String],
  serviceId: Option[String],
  serviceCloudDriverId: Option[String],
  serviceType: Option[String],
  instanceId: Option[String]
)

case class MetricServiceTargets(
  targets: List[String],
  labels: MetricServiceTargetLabels
)


trait CloudDriverService extends Logging {

  def getMetricServiceTargets(): Future[Seq[MetricServiceTargets]]

  def serviceList(): Future[Seq[CloudService]]

  def deployService(service: Service): Future[CloudService]

  def services(serviceIds: Set[Long]): Future[Seq[CloudService]]

  def removeService(serviceId: Long): Future[Unit]
}

object CloudDriverService {
  val LABEL_SERVICE_ID = "SERVICE_ID"
  val LABEL_SERVICE_NAME = "SERVICE_NAME"
  val LABEL_HS_SERVICE_MARKER = "HS_SERVICE_MARKER"
  val LABEL_MODEL_VERSION_ID = "MODEL_VERSION_ID"
  val LABEL_MODEL_VERSION = "MODEL_VERSION"
  val LABEL_MODEL_NAME = "MODEL_NAME"
  val LABEL_MODEL_TYPE = "MODEL_TYPE"
  val LABEL_RUNTIME_ID = "RUNTIME_ID"

  val ENV_APP_PORT = "APP_PORT"
  val ENV_SIDECAR_PORT = "SIDECAR_PORT"
  val ENV_SIDECAR_HOST = "SIDECAR_HOST"

  val ENV_MODEL_DIR = "MODEL_DIR"

  val DEFAULT_MODEL_DIR = "/model"
  val DEFAULT_APP_PORT = 9091
  val DEFAULT_HTTP_PORT = 9090
  val DEFAULT_SIDECAR_INGRESS_PORT = 8080
  val DEFAULT_SIDECAR_EGRESS_PORT = 8081
  val DEFAULT_SIDECAR_ADMIN_PORT = 8082

  val LABEL_DEPLOYMENT_TYPE = "DEPLOYMENT_TYPE"
  val LABEL_ENVIRONMENT = "ENVIRONMENT"

  val DEPLOYMENT_TYPE_MODEL = "MODEL"
  val DEPLOYMENT_TYPE_APP = "APP"
  val DEPLOYMENT_TYPE_SIDECAR = "SIDECAR"

  val PROFILER_ID: Long = -40
  val PROFILER_HTTP_ID: Long = -41
  val MONITORING_ID: Long = -30
  val MONITORING_HTTP_ID: Long = -31
  val MANAGER_ID: Long = -20
  val MANAGER_HTTP_ID: Long = -21
  val MANAGER_UI_ID: Long = -22
  val GATEWAY_ID: Long = -10
  val GATEWAY_HTTP_ID: Long = -11
  val GATEWAY_KAFKA_ID: Long = -12
  val PROFILER_NAME: String = "profiler"
  val PROFILER_HTTP_NAME: String = "profiler-http"
  val MONITORING_NAME: String = "monitoring"
  val MONITORING_HTTP_NAME: String = "monitoring-http"
  val MANAGER_NAME: String = "manager"
  val MANAGER_HTTP_NAME: String = "manager-http"
  val MANAGER_UI_NAME: String = "manager-ui"
  val GATEWAY_NAME: String = "gateway"
  val GATEWAY_HTTP_NAME: String = "gateway-http"
  val GATEWAY_KAFKA_NAME: String = "gateway-kafka"

  val specialIdsByNames = Map(
    PROFILER_NAME -> PROFILER_ID,
    MONITORING_NAME -> MONITORING_ID,
    MANAGER_HTTP_NAME -> MANAGER_HTTP_ID,
    MANAGER_NAME -> MANAGER_ID,
    MANAGER_HTTP_NAME -> MANAGER_HTTP_ID,
    MANAGER_UI_NAME -> MANAGER_UI_ID,
    GATEWAY_NAME -> GATEWAY_ID,
    GATEWAY_HTTP_NAME -> GATEWAY_HTTP_ID,
    GATEWAY_KAFKA_NAME -> GATEWAY_KAFKA_ID
  )

  val fakeHttpServices = Map(
    GATEWAY_ID -> GATEWAY_HTTP_ID,
    PROFILER_ID -> PROFILER_HTTP_ID,
    MONITORING_ID -> MONITORING_HTTP_ID,
    MANAGER_ID -> MANAGER_HTTP_ID
  )

  val specialNamesByIds = Map(
    PROFILER_ID -> PROFILER_NAME,
    PROFILER_HTTP_ID -> PROFILER_HTTP_NAME,
    MONITORING_ID -> MONITORING_NAME,
    MONITORING_HTTP_ID -> MONITORING_HTTP_NAME,
    MANAGER_ID -> MANAGER_NAME,
    MANAGER_HTTP_ID -> MANAGER_HTTP_NAME,
    MANAGER_UI_ID -> MANAGER_UI_NAME,
    GATEWAY_ID -> GATEWAY_NAME,
    GATEWAY_HTTP_ID -> GATEWAY_HTTP_NAME,
    GATEWAY_KAFKA_ID -> GATEWAY_KAFKA_NAME
  )

  def getModelLabels(service: Service): Map[String, String] = {
    val model = service.model.getOrElse(throw new IllegalArgumentException("ModelVersion required"))
    Map[String, String](
      LABEL_SERVICE_ID -> service.id.toString,
      LABEL_HS_SERVICE_MARKER -> LABEL_HS_SERVICE_MARKER,
      LABEL_MODEL_NAME -> model.modelName,
      LABEL_MODEL_VERSION -> model.modelVersion.toString,
      LABEL_MODEL_VERSION_ID -> model.id.toString,
      LABEL_MODEL_TYPE -> model.modelType.toTag,
      CloudDriverService.LABEL_DEPLOYMENT_TYPE -> CloudDriverService.DEPLOYMENT_TYPE_MODEL
    )
  }

  def getRuntimeLabels(service: Service): Map[String, String] =
    Map[String, String](
      CloudDriverService.LABEL_ENVIRONMENT -> service.environment.map(_.name).getOrElse(""),
      LABEL_SERVICE_ID -> service.id.toString,
      LABEL_HS_SERVICE_MARKER -> LABEL_HS_SERVICE_MARKER,
      LABEL_RUNTIME_ID -> service.runtime.id.toString,
      CloudDriverService.LABEL_DEPLOYMENT_TYPE -> CloudDriverService.DEPLOYMENT_TYPE_APP
    )

  def createCloudService(service: Service, statusText: String, cloudDriverId: String, instances: Seq[ServiceInstance]): CloudService =
    CloudService(
      id = service.id,
      serviceName = service.serviceName,
      statusText = statusText,
      cloudDriverId = cloudDriverId,
      environmentName = service.environment.map(e => e.name),
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


  def createFakeHttpServices(services: Seq[CloudService]): Seq[CloudService] =
    services.filter(cs => fakeHttpServices.contains(cs.id))
      .map(cs => {
        val fakeId = fakeHttpServices(cs.id)
        val fakeName = specialNamesByIds(fakeId)

        cs.copy(
          id = fakeId,
          serviceName = fakeName,
          instances = cs.instances.map(s => s.copy(
            advertisedPort = DEFAULT_HTTP_PORT,
            mainApplication = s.mainApplication.copy(port = DEFAULT_HTTP_PORT)
          ))
        )
      })
}
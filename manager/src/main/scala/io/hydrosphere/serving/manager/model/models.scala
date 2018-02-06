package io.hydrosphere.serving.manager.model

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus
import io.hydrosphere.serving.manager.model.ServiceInstanceStatus.ServiceInstanceStatus
import io.hydrosphere.serving.manager.model.api.ModelType

case class Runtime(
  id: Long,
  name: String,
  version: String,
  suitableModelType: List[ModelType],
  tags: List[String],
  configParams: Map[String, String]
) {
  def toImageDef: String = s"$name:$version"
}

case class Model(
  id: Long,
  name: String,
  source: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ModelContract,
  created: LocalDateTime,
  updated: LocalDateTime
)

case class ModelVersion(
  id: Long,
  imageName: String,
  imageTag: String,
  imageSHA256: String,
  created: LocalDateTime,
  modelName: String,
  modelVersion: Long,
  modelType: ModelType,
  source: Option[String],
  model: Option[Model],
  modelContract: ModelContract
) {
  def toImageDef: String = s"$imageName:$imageTag"
}

case class Environment(
  id: Long,
  name: String,
  placeholders: Seq[Any]
)

case class Service(
  id: Long,
  serviceName: String,
  cloudDriverId: Option[String],
  runtime: Runtime,
  model: Option[ModelVersion],
  environment: Option[Environment],
  statusText: String,
  configParams: Map[String, String]
) {
  def toServiceKeyDescription: ServiceKeyDescription =
    ServiceKeyDescription(
      runtimeId = runtime.id,
      modelVersionId = model.map(_.id),
      environmentId = environment.map(_.id)
    )
}

case class ErrorResponse(
  message: String
)

case class ServiceKeyDescription(
  runtimeId: Long,
  modelVersionId: Option[Long],
  environmentId: Option[Long]
) {
  def toServiceName(): String = s"r${runtimeId}m${modelVersionId.getOrElse(0)}e${environmentId.getOrElse(0)}"
}

case class ServiceWeight(
  serviceDescription: ServiceKeyDescription,
  weight: Int
)

case class ApplicationStage(
  services: List[ServiceWeight],
  signatureName: String
)

case class ApplicationExecutionGraph(
  stages: List[ApplicationStage]
)

case class Application(
  id: Long,
  name: String,
  contract: ModelContract,
  executionGraph: ApplicationExecutionGraph
)

object ServiceInstanceStatus extends Enumeration {
  type ServiceInstanceStatus = Value
  val DOWN, UP = Value
}

object ModelBuildStatus extends Enumeration {
  type ModelBuildStatus = Value
  val STARTED, FINISHED, ERROR = Value
}

case class ModelBuildScript(
  name: String,
  script: String
)

class SchematicRuntime(
  name: String,
  version: String,
  suitableModelType: List[ModelType]
) extends Runtime(-1, name, version, suitableModelType, List(), Map())


case class ModelBuild(
  id: Long,
  model: Model,
  version: Long,
  started: LocalDateTime,
  finished: Option[LocalDateTime] = None,
  status: ModelBuildStatus,
  statusText: Option[String],
  logsUrl: Option[String],
  modelVersion: Option[ModelVersion]
)

case class ServiceInstance(
  instanceId: String,
  host: String,
  appPort: Int,
  sidecarPort: Int,
  sidecarAdminPort: Int,
  serviceId: Long,
  status: ServiceInstanceStatus,
  statusText: Option[String]
)

class UnknownModelRuntime extends ModelVersion(
  id = -1,
  imageName = "",
  imageTag = "",
  imageSHA256 = "",
  modelName = "",
  modelVersion = 1,
  source = None,
  modelContract = ModelContract(),
  created = LocalDateTime.now(),
  modelType = ModelType.Unknown("unknown"),
  model = None
)

class AnyEnvironment extends Environment(
  AnyEnvironment.anyEnvironmentId, "Without Env", AnyEnvironment.emptyPlaceholder
)

object AnyEnvironment {
  val emptyPlaceholder = Seq()
  val anyEnvironmentId: Long = -1
}
package io.hydrosphere.serving.manager.model

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus
import io.hydrosphere.serving.manager.model.ModelServiceInstanceStatus.ModelServiceInstanceStatus
import io.hydrosphere.serving.model_api.ModelType
import io.hydrosphere.serving.model.{ModelRuntime, RuntimeType, ServingEnvironment}

object ModelServiceInstanceStatus extends Enumeration {
  type ModelServiceInstanceStatus = Value
  val DOWN, UP = Value
}

object ModelBuildStatus extends Enumeration {
  type ModelBuildStatus = Value
  val STARTED, FINISHED, ERROR = Value
}

case class RuntimeTypeBuildScript(
  name: String,
  version: Option[String],
  script: String
)

class SchematicRuntimeType(
  name: String,
  version: String,
  modelType: ModelType
) extends RuntimeType(-1, name, version, modelType, List(), Map())

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


case class ModelBuild(
  id: Long,
  model: Model,
  modelVersion: String,
  started: LocalDateTime,
  finished: Option[LocalDateTime] = None,
  status: ModelBuildStatus,
  statusText: Option[String],
  logsUrl: Option[String],
  modelRuntime: Option[ModelRuntime],
  runtimeType: Option[RuntimeType]
)

case class ModelFile(
  id: Long,
  path: String,
  model: Model,
  hashSum: String,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime
)

case class ModelServiceInstance(
  instanceId: String,
  host: String,
  appPort: Int,
  sidecarPort: Int,
  sidecarAdminPort: Int,
  serviceId: Long,
  status: ModelServiceInstanceStatus,
  statusText: Option[String]
)

class UnknownModelRuntime extends ModelRuntime(
  id = -1, imageName = "",
  imageTag = "", imageMD5Tag = "",
  modelName = "",modelVersion = "",
  source = None, runtimeType = None,
  modelContract = ModelContract(),
  created = LocalDateTime.now(), modelId = None,
  configParams = Map.empty, tags = List.empty
)

class AnyServingEnvironment extends ServingEnvironment(
  AnyServingEnvironment.anyServingEnvironmentId, "Without Env", AnyServingEnvironment.emptyPlaceholder
)

object AnyServingEnvironment {
  val emptyPlaceholder = Seq()
  val anyServingEnvironmentId:Long = -1
}
package io.hydrosphere.serving.manager.model

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus
import io.hydrosphere.serving.manager.model.ModelServiceInstanceStatus.ModelServiceInstanceStatus

object ModelServiceInstanceStatus extends Enumeration {
  type ModelServiceInstanceStatus = Value
  val DOWN, UP = Value
}

object ModelBuildStatus extends Enumeration {
  type ModelBuildStatus = Value
  val STARTED, FINISHED, ERROR = Value
}

case class RuntimeType(
  id: Long,
  name: String,
  version: String,
  tags: List[String]
)

case class RuntimeTypeBuildScript(
  name: String,
  version: Option[String],
  script: String
)

class SchematicRuntimeType(
  name: String,
  version: String
) extends RuntimeType(-1, name, version, List())

case class Model(
  id: Long,
  name: String,
  source: String,
  runtimeType: Option[RuntimeType],
  description: Option[String],
  outputFields: List[String],
  inputFields: List[String],
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
  modelRuntime: Option[ModelRuntime]
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
  statusText: String
)

case class ModelService(
  serviceId: Long,
  serviceName: String,
  cloudDriverId: Option[String],
  modelRuntime: ModelRuntime,
  status: Option[String],
  statusText: Option[String]
)

case class ModelRuntime(
  id: Long,
  imageName: String,
  imageTag: String,
  imageMD5Tag: String,
  modelName: String,
  modelVersion: String,
  source: Option[String],
  runtimeType: Option[RuntimeType],
  outputFields: List[String],
  inputFields: List[String],
  created: LocalDateTime,
  modelId: Option[Long]
)

class UnknownModelRuntime extends ModelRuntime(
  -1, "", "", "", "", "", None, None, List(), List(), LocalDateTime.now(), None
)

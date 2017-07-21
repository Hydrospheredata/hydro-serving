package io.hydrosphere.serving.manager.model

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus
import io.hydrosphere.serving.manager.model.ModelServiceInstanceStatus.ModelServiceInstanceStatus

object ModelServiceStatus extends Enumeration {
  type ModelServiceInstanceStatus = Value
  val STARTING, ERROR, STARTED = Value
}

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
  version: String
)

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
  started: LocalDateTime,
  finished: Option[LocalDateTime] = None,
  status: ModelBuildStatus,
  statusText: String,
  logsUrl: String
)


case class ModelServiceInstance(
  instanceId: String,
  serviceId: String,
  modelVersion: String,
  status: ModelServiceInstanceStatus,
  statusText: String
)

case class ModelService(
  serviceId: String,
  serviceName: String,
  cloudDriverId: String,
  modelRuntime: ModelRuntime
)

case class ModelRuntime(
  modelRuntimeId: String,
  imageName: String,
  imageTag: String,
  imageMD5Tag: String,
  modelName: String,
  modelVersion: String,
  source: String,
  runtimeType: RuntimeType,
  outputFields: List[String],
  inputFields: List[String],
  buildTimestamp: LocalDateTime
)

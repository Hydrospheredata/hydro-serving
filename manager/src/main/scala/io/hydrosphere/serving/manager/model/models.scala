package io.hydrosphere.serving.manager.model

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus
import io.hydrosphere.serving.manager.model.ModelServiceInstanceStatus.ModelServiceInstanceStatus
import io.hydrosphere.serving.model_api.{DataFrame, ModelApi}
import io.hydrosphere.serving.model.{ModelRuntime, RuntimeType}

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
  version: String
) extends RuntimeType(-1, name, version, List(), Map())

case class Model(
  id: Long,
  name: String,
  source: String,
  runtimeType: Option[RuntimeType],
  description: Option[String],
  outputFields: ModelApi,
  inputFields: ModelApi,
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
  statusText: Option[String]
)

class UnknownModelRuntime extends ModelRuntime(
  -1, "", "", "", "", "", None, None, DataFrame(List.empty), DataFrame(List.empty), LocalDateTime.now(), None, Map(), List()
)

case class ServingEnvironment(
  id: Long,
  name: String,
  placeholders: Seq[Any]
)

class AnyServingEnvironment extends ServingEnvironment(
  -1, "any", Seq()
)
package io.hydrosphere.serving.model

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.model_api.ModelType


case class RuntimeType(
  id: Long,
  name: String,
  version: String,
  modelType: ModelType,
  tags: List[String],
  configParams: Map[String, String]
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
  modelContract: ModelContract,
  created: LocalDateTime,
  modelId: Option[Long],
  configParams: Map[String, String],
  tags: List[String]
)

case class ServingEnvironment(
  id: Long,
  name: String,
  placeholders: Seq[Any]
)

case class ModelService(
  serviceId: Long,
  serviceName: String,
  cloudDriverId: Option[String],
  modelRuntime: ModelRuntime,
  environment: Option[ServingEnvironment],
  status: Option[String],
  statusText: Option[String],
  configParams: Map[String, String]
)

case class ErrorResponse(
  message: String
)

case class ServiceWeight(
  serviceId: Long,
  weight: Int,
  signatureName: String
)

case class ApplicationStage(
  services: List[ServiceWeight]
)

case class ApplicationExecutionGraph(
  stages: List[ApplicationStage]
)

case class Application(
  id: Long,
  name: String,
  executionGraph: ApplicationExecutionGraph,
  sourcesList: List[Long]
)
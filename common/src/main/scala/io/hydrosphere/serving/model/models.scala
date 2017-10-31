package io.hydrosphere.serving.model

import java.time.LocalDateTime


case class RuntimeType(
  id: Long,
  name: String,
  version: String,
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
  outputFields: List[String],
  inputFields: List[String],
  created: LocalDateTime,
  modelId: Option[Long],
  configParams: Map[String, String],
  tags: List[String]
)

case class ModelService(
  serviceId: Long,
  serviceName: String,
  cloudDriverId: Option[String],
  modelRuntime: ModelRuntime,
  status: Option[String],
  statusText: Option[String],
  configParams: Map[String, String]
)

case class ErrorResponse(
  message: String
)

case class ServiceWeight(
  serviceId: Long,
  weight: Int
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
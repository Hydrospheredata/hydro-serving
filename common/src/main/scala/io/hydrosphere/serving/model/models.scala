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

case class Endpoint(
  endpointId: Long,
  name: String,
  currentPipeline: Option[Pipeline]
)

case class Pipeline(
  pipelineId: Long,
  name: String,
  stages: Seq[PipelineStage]
)

case class PipelineStage(
  serviceId: Long,
  serviceName: String,
  servePath: String
)

case class ErrorResponse(
  message: String
)

case class ServiceWeight(
  serviceId: Long,
  weight: Int
)

case class WeightedService(
  id: Long,
  serviceName: String,
  weights: List[ServiceWeight],
  sourcesList: List[Long]
)
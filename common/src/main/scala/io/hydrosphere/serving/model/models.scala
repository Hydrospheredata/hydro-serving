package io.hydrosphere.serving.model

/**
  *
  */

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
package io.hydrosphere.serving.model

/**
  *
  */

case class Endpoint(
  name: String,
  currentPipeline: Option[Pipeline]
)

case class Pipeline(
  name: String,
  stages: Seq[PipelineStage],
  inputFields: Seq[String]
)

case class PipelineStage(
  serviceName: String,
  servePath: String
)

case class ErrorResponse(
  message:String
)
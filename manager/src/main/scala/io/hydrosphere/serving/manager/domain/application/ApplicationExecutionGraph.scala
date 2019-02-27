package io.hydrosphere.serving.manager.domain.application

case class ApplicationExecutionGraph(
  stages: Seq[PipelineStage]
)

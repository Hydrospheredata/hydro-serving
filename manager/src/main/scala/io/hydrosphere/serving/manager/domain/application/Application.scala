package io.hydrosphere.serving.manager.domain.application

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionService, ExecutionStage, KafkaStreaming, Application => GApp}

case class Application(
  id: Long,
  name: String,
  namespace: Option[String],
  contract: ModelContract,
  executionGraph: ApplicationExecutionGraph,
  kafkaStreaming: List[ApplicationKafkaStream]
)
package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.contract.model_contract.ModelContract

case class Application(
  id: Long,
  name: String,
  namespace: Option[String],
  contract: ModelContract,
  executionGraph: ApplicationExecutionGraph,
  kafkaStreaming: List[ApplicationKafkaStream]
)

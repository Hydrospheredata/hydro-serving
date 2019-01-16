package io.hydrosphere.serving.manager.domain.application

import io.hydrosphere.serving.contract.model_signature.ModelSignature

case class Application(
  id: Long,
  name: String,
  namespace: Option[String],
  signature: ModelSignature,
  executionGraph: ApplicationExecutionGraph,
  kafkaStreaming: List[ApplicationKafkaStream]
)
package io.hydrosphere.serving.manager.domain.application

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.domain.application.ApplicationStatus.ApplicationStatus

case class Application(
  id: Long,
  name: String,
  namespace: Option[String],
  status: ApplicationStatus,
  signature: ModelSignature,
  executionGraph: ApplicationExecutionGraph,
  kafkaStreaming: Seq[ApplicationKafkaStream]
)
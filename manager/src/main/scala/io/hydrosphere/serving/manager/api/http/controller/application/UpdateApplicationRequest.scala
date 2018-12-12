package io.hydrosphere.serving.manager.api.http.controller.application

import io.hydrosphere.serving.manager.domain.application.ApplicationKafkaStream

case class UpdateApplicationRequest(
  id: Long,
  name: String,
  namespace: Option[String],
  executionGraph: ExecutionGraphRequest,
  kafkaStreaming: Option[Seq[ApplicationKafkaStream]]
)

object UpdateApplicationRequest {

  import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._

  implicit val format = jsonFormat5(UpdateApplicationRequest.apply)
}
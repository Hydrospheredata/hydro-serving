package io.hydrosphere.serving.manager.api.http.controller.application

import io.hydrosphere.serving.manager.domain.application.ApplicationKafkaStream

case class CreateApplicationRequest(
  name: String,
  namespace: Option[String],
  executionGraph: ExecutionGraphRequest,
  kafkaStreaming: List[ApplicationKafkaStream]
)

object CreateApplicationRequest {
  import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._
  implicit val format = jsonFormat4(CreateApplicationRequest.apply)
}
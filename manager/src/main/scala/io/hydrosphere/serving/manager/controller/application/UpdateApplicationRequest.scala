package io.hydrosphere.serving.manager.controller.application

import io.hydrosphere.serving.manager.model.db.ApplicationKafkaStream

case class UpdateApplicationRequest(
  id: Long,
  name: String,
  namespace: Option[String],
  executionGraph: ExecutionGraphRequest,
  kafkaStreaming: Option[Seq[ApplicationKafkaStream]]
)

object UpdateApplicationRequest {

  import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._

  implicit val format = jsonFormat5(UpdateApplicationRequest.apply)
}
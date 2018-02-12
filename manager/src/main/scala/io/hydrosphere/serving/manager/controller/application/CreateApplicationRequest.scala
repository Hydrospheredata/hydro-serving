package io.hydrosphere.serving.manager.controller.application

import io.hydrosphere.serving.manager.model.ApplicationKafkaStream

case class CreateApplicationRequest(
  name: String,
  executionGraph: ExecutionGraphRequest,
  kafkaStreaming: List[ApplicationKafkaStream]
)

object CreateApplicationRequest {
  import io.hydrosphere.serving.manager.model.CommonJsonSupport._
  implicit val format = jsonFormat3(CreateApplicationRequest.apply)
}
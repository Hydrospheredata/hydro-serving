package io.hydrosphere.serving.manager.controller.application

import io.hydrosphere.serving.manager.model.ApplicationKafkaStream

case class UpdateApplicationRequest (
    id: Long,
    name: String,
    executionGraph: ExecutionGraphRequest,
    kafkaStream: Option[Seq[ApplicationKafkaStream]]
)

object UpdateApplicationRequest {
  import io.hydrosphere.serving.manager.model.CommonJsonSupport._
  implicit val format = jsonFormat4(UpdateApplicationRequest.apply)
}
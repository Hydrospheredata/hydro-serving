package io.hydrosphere.serving.manager.domain.application

case class ApplicationKafkaStream(
  sourceTopic: String,
  destinationTopic: String,
  consumerId: Option[String],
  errorTopic: Option[String]
)

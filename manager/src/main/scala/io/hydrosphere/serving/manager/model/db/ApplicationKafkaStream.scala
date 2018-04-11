package io.hydrosphere.serving.manager.model.db

case class ApplicationKafkaStream(
  sourceTopic: String,
  destinationTopic: String,
  consumerId: Option[String],
  errorTopic: Option[String]
)

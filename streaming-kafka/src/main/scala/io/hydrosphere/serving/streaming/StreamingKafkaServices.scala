package io.hydrosphere.serving.streaming

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.streaming.service.{SidecarServingProcessor, StreamingKafkaConsumer}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class StreamingKafkaServices(
  streamingKafkaConfiguration: StreamingKafkaConfiguration
)(
  implicit val ex: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) {

  val servingProcessor = SidecarServingProcessor(streamingKafkaConfiguration)

  val kafkaStream = {
    import streamingKafkaConfiguration.streaming._
    StreamingKafkaConsumer.kafkaStream(sourceTopic, destinationTopic, servingProcessor)
  }

  kafkaStream.run().onComplete({
    case Success(_) =>
      system.terminate()
    case Failure(e) =>
      system.log.error(e, e.getMessage)
      system.terminate()
  })
}

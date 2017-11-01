package io.hydrosphere.serving.streaming

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffset, CommittableOffsetBatch}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import io.hydrosphere.serving.connector._
import io.hydrosphere.serving.model.CommonJsonSupport
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class ErrorConsumerRecord(
  topic: String,
  partition: Int,
  offset: Long,
  key: Option[String],
  value: Option[String]
)

case class ErrorMessages(
  records: Seq[ErrorConsumerRecord],
  message: String
)

/**
  *
  */
class StreamingKafkaService(
  streamingKafkaConfiguration: StreamingKafkaConfiguration
)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends CommonJsonSupport {
  private implicit val executionContext = system.dispatcher

  private implicit val errorConsumerRecord = jsonFormat5(ErrorConsumerRecord)
  private implicit val errorMessages = jsonFormat2(ErrorMessages)

  private val runtimeMeshConnector: RuntimeMeshConnector = new HttpRuntimeMeshConnector(streamingKafkaConfiguration.sidecar)

  private val destinationTopic = streamingKafkaConfiguration.streaming.destinationTopic

  private val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  private val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)

  //question - we only expect json objects here?
  private def mapAndSend(messages: Seq[CommittableMessage[String, String]]): Future[ExecutionResult] = {
    val batch = messages.map(m => m.record).mkString("[", ",", "]")
    val cmd = ExecutionCommand(
      json = batch.getBytes,
      headers = Seq(),
      pipe = Seq(ExecutionUnit(
        serviceName = streamingKafkaConfiguration.streaming.processorRoute,
        servicePath = "/serve"
      ))
    )
    logger.info(s"TRY INVOKE: $cmd $batch")
    runtimeMeshConnector.execute(cmd)
  }

  private def onFailure(
    throwable: String,
    messages: Seq[CommittableMessage[String, String]]
  ): Future[ProducerMessage.Message[String, String, Seq[CommittableOffset]]] = {

    Future({
      val errors = messages.map(m => {
        import m.record._
        ErrorConsumerRecord(
          topic = topic(),
          partition = partition(),
          offset = offset(),
          key = Option(key()),
          value = Option(value())
        )
      })

      val errMessages = ErrorMessages(errors, throwable)
      ProducerMessage.Message(
        record = new ProducerRecord[String, String](destinationTopic, errMessages.toJson.compactPrint),
        passThrough = messages.map(_.committableOffset)
      )
    })
  }

  private def mapResult(
    executionResult: ExecutionResult,
    messages: Seq[CommittableMessage[String, String]]): Future[ProducerMessage.Message[String, String, Seq[CommittableOffset]]] = {

    executionResult match {
      case ExecutionFailure(err, _) => Future.failed(new RuntimeException(err))
      case ExecutionSuccess(json) =>
        messages.map(m => m.committableOffset)
        val message = ProducerMessage.Message(
          record = new ProducerRecord[String, String](streamingKafkaConfiguration.streaming.destinationTopic, new String(json)),
          passThrough = messages.map(_.committableOffset)
        )
        Future.successful(message)
    }
  }

  //TODO exception handling
  private def sendMessageToMesh(
    messages: Seq[CommittableMessage[String, String]]
  ): Future[ProducerMessage.Message[String, String, Seq[CommittableOffset]]] = {
    mapAndSend(messages).flatMap(
      sentData => mapResult(sentData, messages)
    ) recoverWith {
      case e: Throwable =>
        logger.error(e)
        onFailure(e.getMessage, messages)
    }
  }

  private val done =
    Consumer.committableSource(consumerSettings, Subscriptions.topics(streamingKafkaConfiguration.streaming.sourceTopic))
      .groupedWithin(20, 500 millisecond)
      .mapAsync(3)(dd => sendMessageToMesh(dd))
      .via(Producer.flow(producerSettings))
      .map(m => {
        val offsets = m.message.passThrough
        offsets.foldLeft(CommittableOffsetBatch.empty) { (batch, elem) => batch.updated(elem) }
      })
      .mapAsync(3)(_.commitScaladsl())
      .runWith(Sink.ignore)
      .onComplete {
        case Failure(e) =>
          system.log.error(e, e.getMessage)
          system.terminate()
        case Success(_) => system.terminate()
      }
}

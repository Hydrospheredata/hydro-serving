package io.hydrosphere.serving.streaming

import java.time.LocalDateTime
import java.time.temporal.TemporalUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffset, CommittableOffsetBatch}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import io.hydrosphere.serving.connector.{ManagerConnector, _}
import io.hydrosphere.serving.model.{Application, CommonJsonSupport}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import spray.json._

case class ErrorConsumerRecord(
  topic: String,
  partition: Int,
  offset: Long,
  key: Option[String],
  value: Option[String]
)

case class ErrorMessages(
  records: List[ErrorConsumerRecord],
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

  private val managerConnector: ManagerConnector = new HttpManagerConnector(streamingKafkaConfiguration.manager.host, streamingKafkaConfiguration.manager.port)

  private var application: Option[(Application, LocalDateTime)] = None

  private val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  private val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)

  private def mapAndSend(messages: Seq[CommittableMessage[String, String]], application: Application): Future[ExecutionResult] = {
    Future(messages.map(m => m.record.value().parseJson.convertTo[Any])).flatMap(marshalledMessages =>
      runtimeMeshConnector.execute(ExecutionCommand(
        json = marshalledMessages,
        headers = Seq(),
        pipe = application.executionGraph.stages.indices.map(stage => ExecutionUnit(
          serviceName = s"app${application.id}stage$stage",
          servicePath = "/serve"
        ))
      ))
    )
  }

  private def onFailure(throwable: String, messages: Seq[CommittableMessage[String, String]]): Future[Seq[ProducerMessage.Message[String, String, CommittableOffset]]] = {
    Future({
      val mapMessage = messages.map(s => (s"${s.record.partition()}_${s.record.topic()}", s))
        .groupBy(_._1)
        .mapValues(_.map(_._2))

      val mappedList = mapMessage.map { case (_, msgs) =>
        val errorMessages = ErrorMessages(
          records = msgs.map(m => ErrorConsumerRecord(
            topic = m.record.topic(),
            partition = m.record.partition(),
            offset = m.record.offset(),
            key = Option(m.record.key()),
            value = Option(m.record.value())
          )).toList,
          message = throwable
        )
        msgs.last -> errorMessages
      }.toList

      //Marshal error messages
      mappedList.map(entity => {
        ProducerMessage.Message(
          record = new ProducerRecord[String, String](streamingKafkaConfiguration.streaming.destinationTopic, entity._2.toJson.compactPrint),
          passThrough = entity._1.committableOffset
        )
      })
    })
  }

  private def mapResult(executionResult: ExecutionResult, messages: Seq[CommittableMessage[String, String]]): Future[Seq[ProducerMessage.Message[String, String, CommittableOffset]]] = {
    if (executionResult.status != StatusCodes.OK) {
      return Future.failed(new RuntimeException(executionResult.json.toString()))
    }

    if (executionResult.json.size != messages.size) {
      return Future.failed(new RuntimeException("Wrong result size"))
    }

    Future({
      val zippedMessages = executionResult.json zip messages
      zippedMessages.map(v => {
        ProducerMessage.Message(
          record = new ProducerRecord[String, String](streamingKafkaConfiguration.streaming.destinationTopic, v._1.toJson.compactPrint),
          passThrough = v._2.committableOffset
        )
      })
    })
  }

  private def fetchApplication(): Future[Option[Application]] = {
    if (application.isEmpty || application.get._2.isBefore(LocalDateTime.now().minusSeconds(30))) {
      managerConnector.getApplications
        .map(apps => {
          val findApp = apps.find(a => a.id == streamingKafkaConfiguration.streaming.processorApplication)
          findApp match {
            case _ => findApp
            case Some(x) =>
              this.application = Option((x, LocalDateTime.now()))
              findApp
          }
        })
    } else {
      Future.successful(application.map(r => r._1))
    }
  }

  //TODO batch send
  //TODO exception handling
  private def sendMessageToMesh(messages: Seq[CommittableMessage[String, String]]): Future[Seq[ProducerMessage.Message[String, String, CommittableOffset]]] = {
    fetchApplication().flatMap {
      case None => onFailure(s"Can't find application with id=${streamingKafkaConfiguration.streaming.processorApplication}", messages)
      case Some(x) => mapAndSend(messages, x).flatMap(
        sentData => mapResult(sentData, messages)
      ) recoverWith {
        case e: Throwable =>
          logger.error(e)
          onFailure(e.getMessage, messages)
      }
    }
  }

  private val done =
    Consumer.committableSource(consumerSettings, Subscriptions.topics(streamingKafkaConfiguration.streaming.sourceTopic))
      .groupedWithin(20, 500 millisecond)
      .mapAsync(3)(dd => sendMessageToMesh(dd))
      .mapConcat(c => c.toList)
      .via(Producer.flow(producerSettings))
      .map(_.message.passThrough)
      .batch(max = 20, first => CommittableOffsetBatch.empty.updated(first)) { (batch, elem) =>
        batch.updated(elem)
      }
      .mapAsync(3)(_.commitScaladsl())
      .runWith(Sink.ignore)
      .onComplete {
        case Failure(e) =>
          system.log.error(e, e.getMessage)
          system.terminate()
        case Success(_) => system.terminate()
      }
}

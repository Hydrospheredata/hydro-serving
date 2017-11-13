package io.hydrosphere.serving.streaming.service

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffset, CommittableOffsetBatch}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, GraphDSL, Keep, Merge, RunnableGraph, Sink}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.Future
import scala.concurrent.duration._

case class BatchServingResult(
  records: Seq[ProducerMessage.Message[String, String, Unit]],
  processedOffsets: Seq[CommittableOffset]
)

object StreamingKafkaConsumer {

  def produceAndCommit(settings: ProducerSettings[String, String]): Sink[BatchServingResult, Future[Done]] = {
    Sink.fromGraph(GraphDSL.create(Sink.ignore){ implicit b => sink =>
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[BatchServingResult](2))
      val push = broadcast.out(0)
        .mapConcat(_.records.toList)
        .via(Producer.flow(settings))

      val commit = broadcast.out(1)
        .map(r => {
          r.processedOffsets.foldLeft(CommittableOffsetBatch.empty) {(batch, elem) => batch.updated(elem)}
        })
        .mapAsync(3)(_.commitScaladsl())

      val merge = b.add(Merge[Any](2))

      push ~> merge.in(0)
      commit ~> merge.in(1)

      merge ~> sink.in

      SinkShape(broadcast.in)
    })
  }

  /**
    * Main entry to start kafka serving
    */
  def kafkaStream(topicIn: String, topicOut: String, serving: ServingProcessor)
      (implicit sys: ActorSystem, mat: ActorMaterializer): RunnableGraph[Future[Done]] = {

    implicit val ec = sys.dispatcher

    val consumerSettings = ConsumerSettings(sys, new StringDeserializer, new StringDeserializer)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

    val producerSettings = ProducerSettings(sys, new StringSerializer, new StringSerializer)

    def serve(messages: Seq[CommittableMessage[String, String]]): Future[BatchServingResult] = {
      val input = messages.map(_.record.value())

      serving.serve(input).map(output => {
        val records = output.map(data => ProducerMessage.Message[String, String, Unit](
          record = new ProducerRecord[String, String](topicOut, data),
          passThrough = ()
        ))
        BatchServingResult(records, messages.map(_.committableOffset))
      })
    }

    Consumer.committableSource(consumerSettings, Subscriptions.topics(topicIn))
      .groupedWithin(20, 500 millisecond)
      .mapAsync(3)(serve)
      .toMat(produceAndCommit(producerSettings))(Keep.right)
  }
}

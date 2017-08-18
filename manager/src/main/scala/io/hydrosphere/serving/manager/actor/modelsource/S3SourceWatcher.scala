package io.hydrosphere.serving.manager.actor.modelsource

import java.time.{Instant, LocalDateTime, ZoneId}

import akka.actor.Props
import com.google.common.hash.Hashing
import io.hydrosphere.serving.manager.actor.modelsource.S3SourceWatcher.SQSMessage
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher.{FileCreated, FileDeleted, FileEvent}
import io.hydrosphere.serving.manager.service.modelsource.S3ModelSource
import io.hydrosphere.serving.model.CommonJsonSupport

import scala.collection.JavaConversions._

/**
  * Created by bulat on 04.07.17.
  */
class S3SourceWatcher(val source: S3ModelSource) extends SourceWatcher {
  override def onWatcherTick(): List[FileEvent] = {
    val messages = source
      .configuration
      .sqsClient
      .receiveMessage(source.configuration.queue)
      .getMessages
    val msgBodies = messages.map(m => m -> m.getBody)
      .map { case (m, b) => m -> SQSMessage.fromJson(b) }
      .filter { case (_, b) => b.bucket == source.configuration.bucket }
      .toMap

    msgBodies.toList.flatMap {
      case (message, info) =>
        val event = info.eventName.split(':').head match {
          case "ObjectRemoved" =>
            log.debug(s"ObjectRemoved: ${info.objKey}")
            source.deleteProxyObject(info.objKey)
            if (info.objKey.endsWith("/")) {
              source.cacheSource.getAllFiles(info.objKey).map{ f =>
                FileDeleted(source, f)
              }
            } else {
              List(FileDeleted(source, info.objKey))
            }
          case "ObjectCreated" =>
            log.debug(s"ObjectCreated: ${info.objKey}")
            if (info.objKey.endsWith("/")) {
              source.getAllFiles(info.objKey).map{ f =>
                val fullpath = info.objKey + f
                source.deleteProxyObject(fullpath)
                val file = source.downloadObject(fullpath)
                val hash = com.google.common.io.Files
                  .asByteSource(file)
                  .hash(Hashing.sha256())
                  .toString
                FileCreated(source, fullpath, hash, info.eventTime)
              }
            } else {
              source.deleteProxyObject(info.objKey)
              val file = source.downloadObject(info.objKey)
              val hash = com.google.common.io.Files
                .asByteSource(file)
                .hash(Hashing.sha256())
                .toString
              List(FileCreated(source, info.objKey, hash, info.eventTime))
            }
        }
        source.configuration.sqsClient.deleteMessage(source.configuration.queue, message.getReceiptHandle)
        event
    }
  }
}

object S3SourceWatcher{
  case class SQSMessage(bucket: String, objKey: String, eventName: String, eventTime: LocalDateTime)

  object SQSMessage extends CommonJsonSupport {
    import spray.json._

    def fromJson(json: String): SQSMessage = {
      val map = json.parseJson.convertTo[Map[String, Any]]
      val record = map("Records").asInstanceOf[List[Map[String, Any]]].head
      val s3Data = record("s3").asInstanceOf[Map[String, Any]]
      val bucketData = s3Data("bucket").asInstanceOf[Map[String, Any]]
      val bucketName = bucketData("name").asInstanceOf[String]
      val objectData = s3Data("object").asInstanceOf[Map[String, Any]]
      val objKey = objectData("key").asInstanceOf[String]

      val eventName = record("eventName").asInstanceOf[String]
      val eventTimeStr = record("eventTime").asInstanceOf[String]
      val eventTime = LocalDateTime.ofInstant(Instant.parse(eventTimeStr), ZoneId.systemDefault())
      SQSMessage(bucketName, objKey, eventName, eventTime)
    }

  }

  def props(source: S3ModelSource)=
    Props(classOf[S3SourceWatcher], source)
}
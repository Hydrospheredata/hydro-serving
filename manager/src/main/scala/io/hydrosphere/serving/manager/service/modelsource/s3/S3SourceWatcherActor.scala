package io.hydrosphere.serving.manager.service.modelsource.s3

import java.time.{Instant, LocalDateTime, ZoneId}

import akka.actor.Props
import com.google.common.hash.Hashing
import io.hydrosphere.serving.manager.controller.CommonJsonSupport
import io.hydrosphere.serving.manager.service.modelsource.s3.S3SourceWatcherActor.SQSMessage
import io.hydrosphere.serving.manager.service.modelsource.{FileCreated, FileDeleted, FileEvent, SourceWatcherActor}

import scala.collection.JavaConversions._

/**
  * Created by bulat on 04.07.17.
  */
class S3SourceWatcherActor(val source: S3ModelSource) extends SourceWatcherActor {
  private val sourceDef = source.sourceDef
  override def onWatcherTick(): List[FileEvent] = {
    val messages = sourceDef
      .sqsClient
      .receiveMessage(sourceDef.queue)
      .getMessages
    val msgBodies = messages.map(m => m -> m.getBody)
      .map { case (m, b) => m -> SQSMessage.fromJson(b) }
      .filter { case (_, opt) => opt.isDefined }
      .map { case (m, opt) => m -> opt.get }
      .filter { case (_, b) => b.bucket == sourceDef.bucket }
      .toMap

    msgBodies.toList.flatMap {
      case (message, info) =>
        val event = info.eventName.split(':').head match {
          case "ObjectRemoved" =>
            log.debug(s"ObjectRemoved: ${info.objKey}")
            if (info.objKey.endsWith("/")) {
              val files = source.cacheSource.getAllFiles(info.objKey).map { f =>
                new FileDeleted(source, info.objKey + f, Instant.now())
              }
              source.deleteProxyObject(info.objKey)
              files
            } else {
              source.deleteProxyObject(info.objKey)
              List(new FileDeleted(source, info.objKey, Instant.now()))
            }
          case "ObjectCreated" =>
            log.debug(s"ObjectCreated: ${info.objKey}")
            if (info.objKey.endsWith("/")) {
              source.getAllFiles(info.objKey).map { f =>
                val fullpath = info.objKey + f
                source.deleteProxyObject(fullpath)
                val file = source.downloadObject(fullpath)
                val hash = com.google.common.io.Files
                  .asByteSource(file)
                  .hash(Hashing.sha256())
                  .toString
                new FileCreated(source, fullpath, Instant.now(), hash, info.eventTime)
              }
            } else {
              source.deleteProxyObject(info.objKey)
              val file = source.downloadObject(info.objKey)
              val hash = com.google.common.io.Files
                .asByteSource(file)
                .hash(Hashing.sha256())
                .toString
              List(new FileCreated(source, info.objKey, Instant.now(), hash, info.eventTime))
            }
        }
        sourceDef.sqsClient.deleteMessage(sourceDef.queue, message.getReceiptHandle)
        event
    }
  }
}

object S3SourceWatcherActor{
  case class SQSMessage(bucket: String, objKey: String, eventName: String, eventTime: LocalDateTime)

  object SQSMessage extends CommonJsonSupport {
    import spray.json._

    def fromJson(json: String): Option[SQSMessage] = {
      if(json.parseJson.asJsObject.fields.isEmpty) {
        None
      } else {
        val map = json.parseJson.convertTo[Map[String, Any]]
        for {
          records <- map.get("Records")
          record <- records.asInstanceOf[List[Map[String, Any]]].headOption
          s3 <- record.get("s3")
          s3Data = s3.asInstanceOf[Map[String, Any]]
          bucketData <- s3Data.get("bucket")
          bucketName <- bucketData.asInstanceOf[Map[String, Any]].get("name")
          objectData <- s3Data.get("object")
          objectKey <- objectData.asInstanceOf[Map[String, Any]].get("key")
          eventName <- record.get("eventName")
          eventTime <- record.get("eventTime")
        } yield {
          val eventLocalTime = LocalDateTime.ofInstant(Instant.parse(eventTime.toString), ZoneId.systemDefault())
          SQSMessage(bucketName.toString, objectKey.toString, eventName.toString, eventLocalTime)
        }
      }
    }
  }

  def props(source: S3ModelSource)=
    Props(classOf[S3SourceWatcherActor], source)
}
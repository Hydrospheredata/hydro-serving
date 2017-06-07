package org.kineticcookie.serve

import java.io.{BufferedOutputStream, FileOutputStream}
import java.net.URL
import java.nio.file.{Path, Paths}

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import io.hydrosphere.mist.api.ml.{LocalPipelineModel, PipelineLoader}
import org.apache.spark.ml.PipelineModel
import org.kineticcookie.serve.ModelServer.Message.GetModel
import io.hydrosphere.mist.api.ml._
import LocalPipelineModel._
import SparkMetadata._
import MapAnyJson._
import spray.json._
import DefaultJsonProtocol._

import scala.collection.mutable

/**
  * Created by Bulat on 05.06.2017.
  */
class ModelServer(val repoAddr: String, val repoPort: Int) extends Actor with ActorLogging {
  import context.dispatcher

  private val host = s"$repoAddr:$repoPort"
  private val modelsCache = mutable.Map.empty[String, ModelEntry]
  final implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  log.info(s"Created with $host")

  private def downloadModel(name: String) = {
    val http = Http(context.system)
    import spray.json.DefaultJsonProtocol._
    val fMetadataRequest = http.singleRequest(HttpRequest(uri = s"http://$host/metadata/$name")).flatMap(x => Unmarshal(x.entity).to[SparkMetadata])
    val fFilesRequest = http.singleRequest(HttpRequest(uri = s"http://$host/files/$name")).flatMap(x => Unmarshal(x.entity).to[List[String]])

    fMetadataRequest.zip(fFilesRequest).map{
      case (metadata, files) =>
        log.info(metadata.toString)
        log.info(s"Files: $files")
        files.foreach { filePath =>
          val savePath = s"models/$name/$filePath"
          val folders = Paths.get(savePath).getParent
          new java.io.File(folders.toString).mkdirs()
          val file = new java.io.File(savePath)

          val request = new URL(s"http://$host/download/$name/$filePath")
          val inStream = request.openStream()
          val outStream = new BufferedOutputStream(new FileOutputStream(file))
          val bytes = new Array[Byte](2048)
          var length: Int = 0
          while( length != -1) {
            log.info("Reading...")
            length = inStream.read(bytes)
            if (length != -1) {
              log.info(s"Bytes: $bytes Length: $length")
              outStream.write(bytes, 0, length)
            }
          }
          log.info("Loop exit")
          inStream.close()
          outStream.close()
          log.info(s"Model:$name URL:$filePath File:$savePath")
        }
        val entry = ModelEntry(PipelineLoader.load(s"models/$name"), metadata)
        entry
    }
  }

  override def receive: Receive = {
    case GetModel(name) =>
      val origin = sender()

      modelsCache.get(name) match {
        case Some(model) =>
          log.info(s"Cache hit. Returning model $name")
          origin ! model
        case None =>
          log.info(s"Cache miss. Downloading model $name")
          downloadModel(name).onSuccess{
            case model =>
              modelsCache += name -> model
              log.info(s"Returning downloaded model $name")
              origin ! model
          }
      }
  }
}

object ModelServer {
  def props(repoAddr: String, repoPort: Int) = Props(classOf[ModelServer], repoAddr, repoPort)

  object Message {
    case class GetModel(name: String)
  }
}
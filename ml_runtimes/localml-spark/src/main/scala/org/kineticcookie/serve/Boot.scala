package org.kineticcookie.serve

import java.io.FileOutputStream
import java.net.URL

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings

import scala.concurrent.duration._
import scala.reflect.runtime.universe._
import scala.util.Properties
import java.nio.file._

import io.hydrosphere.mist.api.ml._
import LocalPipelineModel._
import SparkMetadata._
import MapAnyJson._
import spray.json._
import DefaultJsonProtocol._


/**
  * Created by Bulat on 19.05.2017.
  */
object Boot extends App {
  def modelDir(modelName: String): Path = Paths.get(s"models/$modelName")
  def convertCollection[T: TypeTag](list: List[T]) = {
    list match {
      case value: List[Double @unchecked] =>
        value.toArray
      case value: List[Int @unchecked] =>
        value.toArray
      case e => throw new IllegalArgumentException(e.toString)
    }
  }

  implicit val system = ActorSystem("ml_server")
  implicit val materializer = ActorMaterializer()
  implicit val ex = system.dispatcher
  implicit val timeout = Timeout(10.seconds)

  val corsSettings = CorsSettings.defaultSettings

  val routes = cors(corsSettings) {
    get {
     path("health") {
       complete {
         "Hi"
       }
     }~
      path("prepare" / Segment) { modelName =>
        val mlRepoAddr = Properties.envOrSome("ML_REPO_ADDR", Some("192.168.99.100")).get
        val mlRepoPort = Properties.envOrSome("ML_REPO_PORT", Some("8081")).get.toInt

        val fMetadataRequest = Http().singleRequest(HttpRequest(uri = s"http://$mlRepoAddr:$mlRepoPort/metadata/$modelName")).flatMap(x => Unmarshal(x).to[SparkMetadata])
        val fFilesRequest = Http().singleRequest(HttpRequest(uri = s"http://$mlRepoAddr:$mlRepoPort/files/$modelName")).flatMap(x => Unmarshal(x).to[List[String]])

        val modelPath = modelDir(modelName)
        onSuccess(fFilesRequest) { files =>
            Files.deleteIfExists(modelPath)
            Files.createDirectory(modelPath)
            files.foreach { f =>
              val request = new URL(s"http://$mlRepoAddr:$mlRepoPort/download/$modelName/$f")
              val inStream = request.openStream()
              val outStream = new FileOutputStream(s"${modelDir(modelName)}/$f")

              val bytes = new Array[Byte](2048)
              var length: Int = 0
              while( length != -1) {
                length = inStream.read(bytes)
                outStream.write(bytes, 0, length)
              }

              inStream.close()
              outStream.close()
            }
            complete {
              "kek"
            }
        }
      }
    }~
    post {
      path(Segment) { modelName =>
        import MapAnyJson._

          entity(as[List[Map[String, Any]]]) { mapList =>
            ???
//            complete {
//              println(s"Incoming request. Model: $modelName. Params: $mapList")
//              try {
//                val metadata
//                val pipelineModel = PipelineLoader.load(modelPath(modelName))
//                println("Pipeline loaded")
//                val inputCols = pipelineModel.getInputColumns
//                val columns = inputCols.map { colName =>
//                  val colData = mapList.map { col =>
//                    val data = col(colName)
//                    data match {
//                      case l: List[Any] => convertCollection(l)
//                      case x => x
//                    }
//                  }
//                  LocalDataColumn(colName, colData)
//                }
//                val inputLDF = LocalData(columns.toList)
//                println("Local DataFrame created")
//                val result = pipelineModel.transform(inputLDF)
//                println(s"Result: ${result.toMapList}")
//                result.select(pipelineModel.getOutputColumns :_*).toMapList.asInstanceOf[List[Any]]
//              } catch {
//                case e: Exception =>
//                  println(e.toString)
//                  e.getStackTrace.foreach(println)
//                  e.toString
//              }
//            }
          }
      }
    }
  }

  Http().bindAndHandle(routes, "0.0.0.0", 8080)
}

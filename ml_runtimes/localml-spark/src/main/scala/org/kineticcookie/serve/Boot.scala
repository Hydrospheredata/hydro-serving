package org.kineticcookie.serve

import java.io.FileOutputStream
import java.net.URL

import akka.pattern.ask
import akka.actor.{ActorSystem, Props}
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
import scala.util.{Failure, Properties}
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
  def convertCollection[T: TypeTag](list: List[T]) = {
    list match {
      case value: List[Double @unchecked] =>
        value.toArray
      case value: List[Int @unchecked] =>
        value.toArray
      case e => throw new IllegalArgumentException(e.toString)
    }
  }

  val addr = "0.0.0.0"
  val port = 8081

  implicit val system = ActorSystem("ml_server")
  implicit val materializer = ActorMaterializer()
  implicit val ex = system.dispatcher
  implicit val timeout = Timeout(2.minutes)

  val mlRepoAddr = Properties.envOrSome("ML_REPO_ADDR", Some("192.168.99.100")).get
  val mlRepoPort = Properties.envOrSome("ML_REPO_PORT", Some("8081")).get.toInt

  val repo = system.actorOf(ModelServer.props(mlRepoAddr, mlRepoPort))
  val corsSettings = CorsSettings.defaultSettings

  val routes = cors(corsSettings) {
    get {
      path("health") {
        complete {
          "Hi"
        }
      }
    } ~
      post {
        path(Segment) { modelName =>
          import MapAnyJson._
          entity(as[List[Map[String, Any]]]) { mapList =>
            println(s"Incoming request. Model: $modelName. Params: $mapList")
            val f = repo ? ModelServer.Message.GetModel(modelName)
            onSuccess(f.mapTo[ModelEntry]) { model =>
              val pipelineModel = model.pipeline
              val inputs = model.metadata.inputs
              val inputKeys = mapList.head.keys.toList
              if (!inputKeys.containsSlice(inputs)) {
                complete {
                  Failure(new IllegalArgumentException(s"Invalid input data. Received $inputKeys. Required $inputs"))
                }
              } else {
                val columns = inputKeys.map { colName =>
                  val colData = mapList.map { row =>
                    val data = row(colName)
                    data match {
                      case l: List[Any] => convertCollection(l)
                      case x => x
                    }
                  }
                  LocalDataColumn(colName, colData)
                }
                val inputLDF = LocalData(columns)
                val result = pipelineModel.transform(inputLDF)
                complete {
                  result.toMapList.asInstanceOf[List[Any]]
                }
              }
            }
          }
        }
      }
  }
  println(s"Running @ $addr:$port")
  Http().bindAndHandle(routes, addr, port)
}

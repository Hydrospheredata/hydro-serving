package io.hydrosphere.serving.repository

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.BasicDirectives.extractUnmatchedPath
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings
import org.apache.logging.log4j.scala.Logging
import spray.json._
import DefaultJsonProtocol._
import io.hydrosphere.serving.repository.ml.Model
import io.hydrosphere.serving.repository.{Configuration, Messages}
import io.hydrosphere.serving.repository.repository.{IndexEntry, IndexerActor, RepositoryActor}

import scala.concurrent.duration._

/**
  * Created by Bulat on 26.05.2017.
  */

object Boot extends App with Logging {
  implicit val system = ActorSystem("ml_repository")
  implicit val materializer = ActorMaterializer()
  implicit val ex = system.dispatcher
  implicit val timeout = Timeout(10.seconds)

  val config = Configuration()

  logger.info(s"Repository is running @ ${config.address}:${config.port}...")

  val repositoryActor = system.actorOf(RepositoryActor.props, "Repository")

  config.dataSources.foreach {
    case (name, source) =>
      logger.info(s"DataSource detected: $name")
      system.actorOf(IndexerActor.props(source), s"Indexer@$name")
  }

  val corsSettings: CorsSettings.Default = CorsSettings.defaultSettings
  val routes: Route = cors(corsSettings) {
    get {
      path("health") {
        complete {
          "Hi"
        }
      } ~ path("index") {
        val mFuture = repositoryActor ? Messages.RepositoryActor.GetIndex
        onSuccess(mFuture.mapTo[List[Model]]) { models =>
          complete {
            models
          }
        }
      } ~ path("files" / Segment) { modelName =>
        val mFuture = repositoryActor ? Messages.RepositoryActor.GetModelFiles(modelName)
        onSuccess(mFuture.mapTo[Option[List[String]]]) { fRes =>
          rejectEmptyResponse {
            complete {
              fRes
            }
          }
        }
      } ~ path("metadata" / Segment) { modelName =>
        val mFuture = repositoryActor ? Messages.RepositoryActor.GetModelIndexEntry(modelName)
        onSuccess(mFuture.mapTo[Option[IndexEntry]]) { fRes =>
          rejectEmptyResponse {
            complete {
              fRes.map(_.model)
            }
          }
        }
      } ~ pathPrefix("download" / Segment) { modelName =>
        extractUnmatchedPath { path =>
          val pathInput = if (path.startsWithSlash) {
            path.dropChars(1)
          } else path
          val mFuture = repositoryActor ? Messages.RepositoryActor.GetFile(modelName, pathInput.toString())
          onSuccess(mFuture.mapTo[Option[File]]) {
            case Some(file) =>
              getFromFile(file)
            case None => complete {
              HttpResponse(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  }
  Http().bindAndHandle(routes, config.address, config.port)
}

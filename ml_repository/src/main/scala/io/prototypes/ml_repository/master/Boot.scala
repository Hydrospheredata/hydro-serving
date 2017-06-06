package io.prototypes.ml_repository.master

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern._
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.{path, _}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings
import io.prototypes.ml_repository.source.LocalSource
import io.prototypes.ml_repository.watcher.{LocalSourceWatcher, SourceWatcher}
import io.prototypes.ml_repository._

import scala.concurrent.duration._
import java.nio.file._
import java.io._
import java.net.URI

import spray.json._
import DefaultJsonProtocol._
import akka.event.Logging
import akka.http.scaladsl.server.Route

import scala.collection.mutable

/**
  * Created by Bulat on 26.05.2017.
  */

object Boot extends App {
  implicit val system = ActorSystem("ml_repository")
  implicit val materializer = ActorMaterializer()
  implicit val ex = system.dispatcher
  implicit val timeout = Timeout(10.seconds)
  private val watchers = mutable.Buffer.empty[ActorRef]

  println(s"Repository is running @ ${Configuration.Web.address}:${Configuration.Web.port}...")

  val repositoryActor = system.actorOf(RepositoryActor.props, "Repository")

  Configuration.dataSources.foreach {
    case (name, source) =>
      println(s"DataSource detected: $name")
      val watcher = source match {
        case s: LocalSource => system.actorOf(LocalSourceWatcher.props(s), s"LocalWatcher@$name")
        case _ => throw new IllegalArgumentException("Unknown watcher")
      }
      watcher ! Messages.Watcher.Subscribe(repositoryActor)
      watchers += watcher
  }
  repositoryActor ! Messages.RepositoryActor.SubscribeWatchers(watchers)

  repositoryActor ! Messages.RepositoryActor.MakeIndex

  val corsSettings: CorsSettings.Default = CorsSettings.defaultSettings
  val routes: Route = cors(corsSettings) {
    get {
      path("health") {
        complete {
          "Hi"
        }
      } ~ path("files" / Segment) { modelName =>
        val mFuture = repositoryActor ? Messages.RepositoryActor.GetModelFiles(modelName)
        onSuccess(mFuture) { fRes =>
          complete {
            fRes.asInstanceOf[Option[List[URI]]].map(_.map(_.toString))
          }
        }
      }~ path("metadata" / Segment) { modelName =>
        val mFuture = repositoryActor ? Messages.RepositoryActor.GetModelIndexEntry(modelName)
        onSuccess(mFuture) { fRes =>
          complete {
            fRes.asInstanceOf[Option[IndexEntry]].map(_.model)
          }
        }
      } ~ pathPrefix("download" / Segment) { modelName =>
        val mFuture = repositoryActor ? Messages.RepositoryActor.GetModelDirectory(modelName)
        onSuccess(mFuture) { fRes =>
          getFromDirectory(fRes.asInstanceOf[Path].toString)
        }
      }
    }
  }
  Http().bindAndHandle(routes, Configuration.Web.address, Configuration.Web.port)
}

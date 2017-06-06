package io.prototypes.ml_repository

import java.io._
import java.nio.file._
import java.net.URI

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

import akka.pattern._
import akka.util.Timeout
import io.prototypes.ml_repository.source.ModelSource

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by Bulat on 26.05.2017.
  */
class RepositoryActor extends Actor {
  import context._

  implicit val timeout = Timeout(10.seconds)
  private val index = mutable.Map.empty[String, IndexEntry]
  val log = Logging(context.system, this)

  override def receive: Receive = waiting

  def waiting: Receive = {
    case Messages.RepositoryActor.SubscribeWatchers(refs) =>
      context become ready(refs)
  }

  def ready(watchers: Seq[ActorRef]): Receive = {
    case Messages.RepositoryActor.MakeIndex =>
      watchers.foreach(_ ! Messages.Watcher.GetModels)

    case Messages.Watcher.IndexedModels(models) =>
      models.foreach{ m =>
        log.info(s"New model: $m")
        index += m.model.name -> m
      }

    case Messages.RepositoryActor.GetModelIndexEntry(name) =>
      sender() ! index.get(name)

    case Messages.RepositoryActor.GetModelFiles(name) =>
      val origin = sender()
      index.get(name).foreach{ indexEntry =>
        log.info("GetModelFiles")
        val f = indexEntry.watcher ? Messages.RepositoryActor.RetrieveModelFiles(indexEntry)
        f.onComplete{
          case Success(s) =>
            val uris = s.asInstanceOf[List[URI]]
            log.info(s"GetModelFiles result: $uris")
            origin !  Some(s.asInstanceOf[List[URI]])
          case Failure(ex) =>
            log.warning(s"GetModelFiles: $ex")
            origin !  None
        }
      }
    case Messages.RepositoryActor.GetModelDirectory(name) =>
      val origin = sender()
      index.get(name).foreach { indexEntry =>
        log.info(s"GetModelDirectory($name)")
        val f = indexEntry.watcher ? Messages.Watcher.GetModelDirectory(indexEntry)
        f.onComplete{
          case Success(s) =>
            val result = s.asInstanceOf[Path]
            origin ! result
          case Failure(ex) =>
            log.warning(s"GetModelDirectory: $ex")
            origin ! None
        }
      }
  }
}

object RepositoryActor {
  def props: Props = Props[RepositoryActor]
}
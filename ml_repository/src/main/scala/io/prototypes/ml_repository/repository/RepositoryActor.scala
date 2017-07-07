package io.prototypes.ml_repository.repository

import java.io.File
import java.nio.file._

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import io.prototypes.ml_repository.Messages

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Created by Bulat on 26.05.2017.
  */
class RepositoryActor extends Actor with ActorLogging {

  private[this] implicit val timeout = Timeout(10.seconds)
  private[this] val index = mutable.Map.empty[String, IndexEntry]

  private def withIndexEntry[R](name: String)(func: (IndexEntry)=>R): Option[R] = {
    index.get(name) match {
      case Some(indexEntry) =>
        Some(func(indexEntry))
      case None =>
        log.warning(s"Index for $name is not found")
        None
    }
  }

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[Messages.Indexer.IndexedModels])
    context.system.eventStream.publish(Messages.Indexer.Index)
  }

  override def receive: Receive = {
    case Messages.Indexer.IndexedModels(models) =>
      models.foreach { m =>
        log.info(s"New model: $m")
        index += m.model.name -> m
      }

    case Messages.RepositoryActor.GetIndex =>
      val origin = sender()
      val models = index.map {
        case (_, entry) => entry.model
      }.toList
      origin ! models

    case Messages.RepositoryActor.GetModelIndexEntry(name) =>
      sender() ! index.get(name)

    case Messages.RepositoryActor.GetModelFiles(name) =>
      val origin = sender()
      log.info("GetModelFiles")
      val result = withIndexEntry(name) {idx => idx.source.getAllFiles(idx.model.runtime, idx.model.name)}
      origin ! result

    case Messages.RepositoryActor.GetFile(modelName, filePath) =>
      val origin = sender()
      log.info(s"GetModelDirectory($modelName, $filePath)")
      val result = withIndexEntry(modelName) { indexEntry =>
        indexEntry.source.getReadableFile(indexEntry.model.runtime, indexEntry.model.name, filePath)
      }
      origin ! result
  }
}

object RepositoryActor {
  def props: Props = Props(classOf[RepositoryActor])
}
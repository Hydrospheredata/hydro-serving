package io.prototypes.ml_repository.datasource.local

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._

import akka.actor.{ActorRef, Props}
import akka.util.Timeout
import io.prototypes.ml_repository.Messages
import io.prototypes.ml_repository.datasource.SourceWatcher
import io.prototypes.ml_repository.utils.FileUtils._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._


/**
  * Created by Bulat on 31.05.2017.
  */
class LocalSourceWatcher(val source: LocalSource, val indexer: ActorRef) extends SourceWatcher {
  import context._

  private[this] val watcher = FileSystems.getDefault.newWatchService()
  private[this] val keys = mutable.Map.empty[WatchKey, Path]

  private def subscribeRecursively(dirFile: File): Unit = {
    val maxDepth = 2
    def _subscribe(dirFile: File, depth: Long = 0): Unit = {
      if (depth <= maxDepth) {
        val dirPath = Paths.get(dirFile.getCanonicalPath).normalize()
        keys += dirPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY) -> dirPath
        dirFile.getSubDirectories.foreach(d => _subscribe(d, depth+1))
      }
    }
    _subscribe(dirFile)
  }

  private def handleWatcherEvent[T](path: Path, event: WatchEvent[T]) = {
    event.kind() match {
      case StandardWatchEventKinds.OVERFLOW =>
        log.warning(s"Overflow: $event")

      case StandardWatchEventKinds.ENTRY_CREATE =>
        val name = event.context().asInstanceOf[Path]
        val child = path.resolve(name)
        log.debug(s"ENTRY_CREATE: $child")
        indexer ! Messages.Watcher.ChangeDetected

      case StandardWatchEventKinds.ENTRY_MODIFY =>
        val name = event.context().asInstanceOf[Path]
        val child = path.resolve(name)
        log.debug(s"ENTRY_MODIFY: $child")
        indexer ! Messages.Watcher.ChangeDetected

      case StandardWatchEventKinds.ENTRY_DELETE =>
        val name = event.context().asInstanceOf[Path]
        val child = path.resolve(name)
        log.debug(s"ENTRY_DELETE: $child")
        indexer ! Messages.Watcher.ChangeDetected

    }
    ///context.system.eventStream.publish(Messages.Watcher.IndexedModels(???))
  }

  override def preStart(): Unit = {
    log.info(s"LocalWatchService with: $source")
    subscribeRecursively(source.sourceFile)
  }

  override def childPostStop(): Unit = {
    keys.keys.foreach(_.cancel())
    watcher.close()
  }

  override def receive: Receive = {
    case Messages.Watcher.Tick =>
      for {
        (key, path) <- keys
        event <- key.pollEvents().asScala
      } {
        handleWatcherEvent(path, event)
      }
  }
}

object LocalSourceWatcher {
  def props(localSource: LocalSource, indexer: ActorRef) = Props(classOf[LocalSourceWatcher], localSource, indexer)
}
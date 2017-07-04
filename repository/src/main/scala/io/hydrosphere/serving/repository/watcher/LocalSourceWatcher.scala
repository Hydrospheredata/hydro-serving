package io.hydrosphere.serving.repository.watcher

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._

import akka.actor.Props
import akka.util.Timeout
import io.hydrosphere.serving.repository.runtime.RuntimeDispatcher
import io.hydrosphere.serving.repository.source.LocalSource
import io.hydrosphere.serving.repository.utils.FileUtils._
import io.hydrosphere.serving.repository.{IndexEntry, Messages}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._


/**
  * Created by Bulat on 31.05.2017.
  */
class LocalSourceWatcher(val source: LocalSource) extends SourceWatcher {

  import context._

  implicit private[this] val timeout = Timeout(10.seconds)
  private[this] val tick = context.system.scheduler.schedule(0.seconds, 500.millis, self, Messages.Watcher.LookForChanges)
  private[this] val watcher = FileSystems.getDefault.newWatchService()
  private[this] val keys = mutable.Map.empty[WatchKey, Path]
  private[this] val sourceFile = new File(source.path.toString)

  private def subscribeRecursively(dirFile: File): Unit = {
    val maxDepth = 2

    def _subscribe(dirFile: File, depth: Long = 0): Unit = {
      if (depth <= maxDepth) {
        val dirPath = Paths.get(dirFile.getCanonicalPath).normalize()
        keys += dirPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY) -> dirPath
        dirFile.getSubDirectories.foreach(d => _subscribe(d, depth + 1))
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
        self ! Messages.Watcher.GetModels

      case StandardWatchEventKinds.ENTRY_MODIFY =>
        val name = event.context().asInstanceOf[Path]
        val child = path.resolve(name)
        log.debug(s"ENTRY_MODIFY: $child")
        self ! Messages.Watcher.GetModels

      case StandardWatchEventKinds.ENTRY_DELETE =>
        val name = event.context().asInstanceOf[Path]
        val child = path.resolve(name)
        log.debug(s"ENTRY_DELETE: $child")
        self ! Messages.Watcher.GetModels

    }
    ///context.system.eventStream.publish(Messages.Watcher.IndexedModels(???))
  }

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, Messages.Watcher.GetModels.getClass)
    log.info(s"LocalWatchService with: ${sourceFile.getCanonicalPath}")
    subscribeRecursively(sourceFile)
    self ! Messages.Watcher.GetModels
  }

  override def postStop(): Unit = tick.cancel()

  override def receive: Receive = {
    case Messages.Watcher.GetModels =>
      val models = RuntimeDispatcher.getModels(sourceFile.getSubDirectories)
      val indexed = models.map { m =>
        IndexEntry(m, self, source)
      }
      context.system.eventStream.publish(Messages.Watcher.IndexedModels(indexed))

    case Messages.Watcher.LookForChanges =>
      for {
        (key, path) <- keys
        event <- key.pollEvents().asScala
      } {
        handleWatcherEvent(path, event)
      }

    case Messages.RepositoryActor.RetrieveModelFiles(index) =>
      val origin = sender()
      log.info(s"RetrieveModel($index)")
      val modelPath = Paths.get(sourceFile.toString, index.model.runtime, index.model.name)
      val modelFiles = modelPath.toFile.listFilesRecursively
      log.info(s"RetrieveModel result: $modelFiles")
      origin ! modelFiles.map(u => modelPath.toUri.relativize(u.toURI))

    case Messages.Watcher.GetModelDirectory(indexEntry) =>
      val origin = sender()
      log.info(s"GetModelDirectory($indexEntry)")
      val modelDirPath = Paths.get(sourceFile.toString, indexEntry.model.runtime, indexEntry.model.name)
      origin ! modelDirPath
  }
}

object LocalSourceWatcher {
  def props(localSource: LocalSource) = Props(classOf[LocalSourceWatcher], localSource)
}
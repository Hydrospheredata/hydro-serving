package io.hydrosphere.serving.manager.actor.modelsource

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._
import java.time.LocalDateTime

import akka.actor.Props
import com.google.common.hash.Hashing
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher.{FileCreated, FileDeleted, FileEvent, FileModified}
import io.hydrosphere.serving.manager.service.modelsource.LocalModelSource
import io.hydrosphere.serving.util.FileUtils._

import scala.collection.JavaConverters._
import scala.collection.mutable


/**
  * Created by Bulat on 31.05.2017.
  */
class LocalSourceWatcher(val source: LocalModelSource) extends SourceWatcher {
  private[this] val watcher = FileSystems.getDefault.newWatchService()
  private[this] val keys = mutable.Map.empty[WatchKey, Path]

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

  private def handleWatcherEvent[T](path: Path, event: WatchEvent[T]): Option[FileEvent] = {
    val name = event.context().asInstanceOf[Path]
    val child = path.resolve(name)
    val file = child.relativize(source.sourceFile.toPath).toString

    event.kind() match {
      case StandardWatchEventKinds.OVERFLOW =>
        log.warning(s"File system event: Overflow: $event")
        None

      case StandardWatchEventKinds.ENTRY_CREATE =>
        log.debug(s"File system event: ENTRY_CREATE: $child")
        val hash = com.google.common.io.Files.asByteSource(child.toFile).hash(Hashing.sha256()).toString
        Some(FileCreated(source, file, hash, LocalDateTime.now()))

      case StandardWatchEventKinds.ENTRY_MODIFY =>
        log.debug(s"File system event: ENTRY_MODIFY: $child")
        val hash = com.google.common.io.Files.asByteSource(child.toFile).hash(Hashing.sha256()).toString
        Some(FileModified(source, file, hash, LocalDateTime.now()))

      case StandardWatchEventKinds.ENTRY_DELETE =>
        log.debug(s"File system event: ENTRY_DELETE: $child")
        Some(FileDeleted(source, file))

      case x =>
        log.warning(s"File system event: Unknown: $x")
        None
    }
  }

  override def preStart(): Unit = {
    subscribeRecursively(new File(source.configuration.path))
  }

  override def watcherPostStop(): Unit = {
    keys.keys.foreach(_.cancel())
    watcher.close()
  }

  override def onWatcherTick(): List[FileEvent] = {
    val events = for {
      (key, path) <- keys
      event <- key.pollEvents().asScala
    } yield {
      handleWatcherEvent(path, event)
    }
    events.filter(_.isDefined).map(_.get).toList
  }
}

object LocalSourceWatcher{
  def props(source: LocalModelSource)=
    Props(classOf[LocalSourceWatcher], source)
}
package io.hydrosphere.serving.manager.service.modelsource.local

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.time.{Instant, LocalDateTime, ZoneId}

import akka.actor.Props
import com.google.common.hash.Hashing
import io.hydrosphere.serving.manager.service.modelsource._
import io.hydrosphere.serving.manager.util.FileUtils._

import scala.collection.JavaConverters._
import scala.collection.mutable


/**
  * Created by Bulat on 31.05.2017.
  */
class LocalSourceWatcherActor(val source: LocalModelSource) extends SourceWatcherActor {
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

  private def handleWatcherEvent[T](path: Path, event: WatchEvent[T]): List[FileEvent] = {
    def handleCreation(relativePath: Path) = {
      val absolutePath = source.sourceFile.toPath.resolve(relativePath)
      val hash = com.google.common.io.Files
        .asByteSource(absolutePath.toFile)
        .hash(Hashing.sha256())
        .toString
      val attributes = Files.readAttributes(absolutePath, classOf[BasicFileAttributes])
      new FileCreated(
        source,
        relativePath.toString,
        Instant.now(),
        hash,
        LocalDateTime.ofInstant(attributes.creationTime().toInstant, ZoneId.systemDefault())
      )
    }

    val filename = event.context().asInstanceOf[Path]
    val absolutePath = source.sourceFile.toPath.resolve(filename)
    val file = absolutePath.toFile
    val relativePath = source.sourceFile.toPath.relativize(absolutePath)

    if (file.isHidden) {
      Nil
    } else {
      event.kind() match {
        case StandardWatchEventKinds.OVERFLOW =>
          log.warning(s"File system event: Overflow: $event")
          Nil

        case StandardWatchEventKinds.ENTRY_CREATE =>
          log.debug(s"File system event: ENTRY_CREATE: $relativePath")
          if (Files.isDirectory(absolutePath)) {
            file
              .listFilesRecursively
              .map(_.toPath)
              .map(source.sourceFile.toPath.relativize)
              .map(handleCreation)
              .toList
          } else {
            List(handleCreation(relativePath))
          }

        case StandardWatchEventKinds.ENTRY_MODIFY =>
          log.debug(s"File system event: ENTRY_MODIFY: $relativePath")
          val hash = com.google.common.io.Files
            .asByteSource(absolutePath.toFile)
            .hash(Hashing.sha256())
            .toString
          val lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(absolutePath).toInstant, ZoneId.systemDefault())
          List(new FileModified(source, relativePath.toString, Instant.now(), hash, lastModified))

        case StandardWatchEventKinds.ENTRY_DELETE =>
          log.debug(s"File system event: ENTRY_DELETE: $relativePath")
          List(new FileDeleted(source, relativePath.toString, Instant.now()))
        case x =>
          log.warning(s"File system event: Unknown: $x")
          Nil
      }
    }
  }

  override def watcherPreStart(): Unit = {
    subscribeRecursively(source.sourceFile)
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

    events.flatten.toList
  }
}

object LocalSourceWatcherActor{
  def props(source: LocalModelSource)=
    Props(classOf[LocalSourceWatcherActor], source)
}
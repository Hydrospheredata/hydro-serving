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

  private def handleWatcherEvent[T](path: Path, event: WatchEvent[T]): Seq[FileEvent] = {
    val filename = event.context().asInstanceOf[Path]
    val absolutePath = path.resolve(filename)

    if (absolutePath.toFile.isHidden) {
      Nil
    } else {
      event.kind() match {
        case StandardWatchEventKinds.OVERFLOW =>
          log.warning(s"File system event: Overflow: $event")
          Nil

        case StandardWatchEventKinds.ENTRY_CREATE =>
          log.debug(s"File system event: ENTRY_CREATE: $absolutePath")
          handleCreation(absolutePath)

        case StandardWatchEventKinds.ENTRY_MODIFY =>
          log.debug(s"File system event: ENTRY_MODIFY: $absolutePath")
          handleModification(absolutePath)

        case StandardWatchEventKinds.ENTRY_DELETE =>
          log.debug(s"File system event: ENTRY_DELETE: $absolutePath")
          handleDeletion(absolutePath)

        case x =>
          log.warning(s"File system event: Unknown: $x")
          Nil
      }
    }
  }

  private def handleDeletion[T](absolutePath: Path) = {
    List(
      FileDeleted(
        source,
        source.sourceFile.toPath.relativize(absolutePath).toString,
        Instant.now()
      )
    )
  }

  private def handleModification[T](absolutePath: Path) = {
    if (Files.isDirectory(absolutePath)) {
      absolutePath.toFile
        .listFilesRecursively
        .map(_.toPath)
        .map(handleFileModification)
        .toList
    } else {
      Seq(handleFileModification(absolutePath))
    }
  }


  private def handleFileModification(absolutePath: Path) = {
    val hash = com.google.common.io.Files
      .asByteSource(absolutePath.toFile)
      .hash(Hashing.sha256())
      .toString
    val lastModified = LocalDateTime.ofInstant(
      Files.getLastModifiedTime(absolutePath).toInstant,
      ZoneId.systemDefault()
    )
    FileModified(
      source,
      source.sourceFile.toPath.relativize(absolutePath).toString,
      Instant.now(),
      hash,
      lastModified
    )
  }

  private def handleCreation(absolutePath: Path) = {
    if (Files.isDirectory(absolutePath)) {
      absolutePath.toFile
        .listFilesRecursively
        .map(_.toPath)
        .map(handleFileCreation)
        .toList
    } else {
      List(handleFileCreation(absolutePath))
    }
  }

  private def handleFileCreation(absolutePath: Path): FileCreated = {
    val hash = com.google.common.io.Files
      .asByteSource(absolutePath.toFile)
      .hash(Hashing.sha256())
      .toString
    val attributes = Files.readAttributes(absolutePath, classOf[BasicFileAttributes])
    FileCreated(
      source,
      source.sourceFile.toPath.relativize(absolutePath).toString,
      Instant.now(),
      hash,
      LocalDateTime.ofInstant(attributes.creationTime().toInstant, ZoneId.systemDefault())
    )
  }

  override def watcherPreStart(): Unit = {
    subscribeRecursively(source.sourceFile)
  }

  override def watcherPostStop(): Unit = {
    keys.keys.foreach(_.cancel())
    watcher.close()
  }

  override def onWatcherTick(): Seq[FileEvent] = {
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
    Props(new LocalSourceWatcherActor(source))
}
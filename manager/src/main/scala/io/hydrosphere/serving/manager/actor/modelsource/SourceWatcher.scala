package io.hydrosphere.serving.manager.actor.modelsource

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.google.common.hash.Hashing
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher._
import io.hydrosphere.serving.manager.service.modelsource.{LocalModelSource, ModelSource, S3ModelSource}

import scala.concurrent.duration._

/**
  * Created by Bulat on 31.05.2017.
  */
trait SourceWatcher extends Actor with ActorLogging {
  import context._
  implicit private val timeout = Timeout(10.seconds)
  private val timer = context.system.scheduler.schedule(0.seconds, 500.millis, self, Tick)

  /**
    * Tick handler. Every tick Watcher looks for changes in datasource.
    */
  def onWatcherTick(): List[FileEvent]

  def watcherPreStart(): Unit = {}

  /**
    * Custom stop logic
    */
  def watcherPostStop(): Unit = {}

  def source: ModelSource

  private final def watcherTick: PartialFunction[Any, Unit] = {
    case Tick =>
      onWatcherTick().foreach(context.system.eventStream.publish)
  }

  final override def receive: Receive = {
    watcherTick orElse {
      case x => log.warning(s"Unknown SourceWatcher message: $x")
    }
  }

  final override def preStart(): Unit = {
    source
      .getSubDirs
      .flatMap( f => source.getAllFiles(f).map(f + "/" + _))
      .map(f => f -> source.getReadableFile(f))
      .map{ case (fileName, file) =>
        val hash = com.google.common.io.Files.asByteSource(file).hash(Hashing.sha256()).toString
        FileDetected(source, fileName, hash)
      }
      .foreach(context.system.eventStream.publish)

    watcherPreStart()
  }

  final override def postStop(): Unit = {
    timer.cancel()
    watcherPostStop()
  }
}

object SourceWatcher {
  case object Tick

  trait FileEvent
  case class FileDetected(source: ModelSource, filename: String, hash: String) extends FileEvent
  case class FileDeleted(source: ModelSource, fileName: String) extends FileEvent
  case class FileCreated(source: ModelSource, fileName: String, hash: String, createdAt: LocalDateTime) extends FileEvent
  case class FileModified(source: ModelSource, fileName: String, hash: String, updatedAt: LocalDateTime) extends FileEvent

  def props(modelSource: ModelSource): Props = {
    modelSource match {
      case x: LocalModelSource =>
        LocalSourceWatcher.props(x)
      case x: S3ModelSource =>
        S3SourceWatcher.props(x)
    }
  }
}


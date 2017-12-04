package io.hydrosphere.serving.manager.actor.modelsource

import java.time.{Instant, LocalDateTime}

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.google.common.hash.Hashing
import io.hydrosphere.serving.manager.actor.{FileDetected, FileEvent}
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher._
import io.hydrosphere.serving.manager.service.modelsource.s3.S3ModelSource
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.service.modelsource.local.LocalModelSource

import scala.concurrent.duration._

/**
  * Created by Bulat on 31.05.2017.
  */
trait SourceWatcher extends Actor with ActorLogging {
  import context._
  implicit private val timeout = Timeout(30.seconds)
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
        new FileDetected(source, fileName, Instant.now(), hash)
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

  def props(modelSource: ModelSource): Props = {
    modelSource match {
      case x: LocalModelSource =>
        LocalSourceWatcher.props(x)
      case x: S3ModelSource =>
        S3SourceWatcher.props(x)
    }
  }
}


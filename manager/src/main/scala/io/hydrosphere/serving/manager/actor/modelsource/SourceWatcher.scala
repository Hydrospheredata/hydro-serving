package io.hydrosphere.serving.manager.actor.modelsource

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import io.hydrosphere.serving.manager.actor.IndexerActor.Index
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher._
import io.hydrosphere.serving.manager.service.modelsource.ModelSource

import scala.concurrent.duration._

/**
  * Created by Bulat on 31.05.2017.
  */
object SourceWatcher {
  case object Tick

  trait FileEvent

  sealed case class FileDeleted(name: String) extends FileEvent
  sealed case class FileCreated(name: String) extends FileEvent
  sealed case class FileModified(name: String) extends FileEvent
}

trait SourceWatcher extends Actor with ActorLogging {
  import context._
  implicit private val timeout = Timeout(10.seconds)
  private val timer = context.system.scheduler.schedule(0.seconds, 500.millis, self, Tick)

  /**
    * Tick handler. Every tick Watcher looks for changes in datasource.
    */
  def onWatcherTick(): List[FileEvent]

  /**
    * Custom stop logic
    */
  def watcherPostStop(): Unit = {}

  def source: ModelSource

  def indexer: ActorRef

  private final def watcherTick: PartialFunction[Any, Unit] = {
    case Tick =>
      val events = onWatcherTick()
      if (events.nonEmpty) {
        indexer ! Index
      }
  }

  final override def receive: Receive = {
    watcherTick orElse{
      case x => log.warning(s"Unknown SourceWatcher message: $x")
    }
  }

  final override def postStop(): Unit = {
    timer.cancel()
    watcherPostStop()
  }
}

package io.hydrosphere.serving.repository.datasource

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import io.hydrosphere.serving.repository.Messages

import scala.concurrent.duration._

/**
  * Created by Bulat on 31.05.2017.
  */
trait SourceWatcher extends Actor with ActorLogging {
  import context._
  implicit private val timeout = Timeout(10.seconds)
  private val timer = context.system.scheduler.schedule(0.seconds, 500.millis, self, Messages.Watcher.Tick)

  /**
    * Tick handler. Every tick Watcher looks for changes in datasource.
    */
  def onWatcherTick()

  def watcherPostStop(): Unit = {}

  private final def watcherTick: PartialFunction[Any, Unit] = {
    case Messages.Watcher.Tick =>
      onWatcherTick()
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

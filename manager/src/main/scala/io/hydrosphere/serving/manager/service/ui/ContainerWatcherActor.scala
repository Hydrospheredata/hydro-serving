package io.hydrosphere.serving.manager.service.ui

import akka.actor.{ActorRef, Props}
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.ModelService
import io.hydrosphere.serving.manager.service.actors.SelfScheduledActor
import io.hydrosphere.serving.manager.service.ui.ContainerWatcherActor._

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._

class ContainerWatcherActor(timeout: Timeout)
  extends SelfScheduledActor(0.seconds, 1000.millis)(timeout) {

  private[this] val starts = TrieMap.empty[ActorRef, ModelService]
  private[this] val stops = TrieMap.empty[ActorRef, ModelService]


  override def recieveNonTick: Receive = {
    case WatchForStart(service) =>
      val origin = sender()
      starts += origin -> service

    case WatchForStop(service) =>
      val origin = sender()
      stops += origin -> service
  }

  override def onTick(): Unit = {
    starts.foreach{
      case (receiver, service) =>
        receiver ! Started(service)
    }
    stops.foreach{
      case (receiver, service) =>
        receiver ! Stopped(service)
    }
  }
}

object ContainerWatcherActor {
  case class WatchForStart(modelService: ModelService)
  case class Started(modelService: ModelService)

  case class WatchForStop(modelService: ModelService)
  case class Stopped(modelService: ModelService)

  def props(implicit timeout: Timeout) = Props(classOf[ContainerWatcherActor], timeout)
}
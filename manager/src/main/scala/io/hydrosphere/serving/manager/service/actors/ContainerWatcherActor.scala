package io.hydrosphere.serving.manager.service.actors

import akka.actor.{ActorRef, Props}
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.Service
import io.hydrosphere.serving.manager.service.actors.ContainerWatcherActor._

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._

/*
private val containerWatcher = actorSystem.actorOf(ContainerWatcherActor.props)

val f = containerWatcher ? WatchForStart(res)
                f.mapTo[Started].map(_.modelService)
*/

class ContainerWatcherActor(timeout: Timeout)
  extends SelfScheduledActor(0.seconds, 1000.millis)(timeout) {

  private[this] val starts = TrieMap.empty[ActorRef, Service]
  private[this] val stops = TrieMap.empty[ActorRef, Service]


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
  case class WatchForStart(modelService: Service)
  case class Started(modelService: Service)

  case class WatchForStop(modelService: Service)
  case class Stopped(modelService: Service)

  def props(implicit timeout: Timeout) = Props(new ContainerWatcherActor(timeout))
}
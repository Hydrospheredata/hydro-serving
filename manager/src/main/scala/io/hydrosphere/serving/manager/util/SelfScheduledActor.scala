package io.hydrosphere.serving.manager.util

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import io.hydrosphere.serving.manager.util.SelfScheduledActor.Tick

import scala.concurrent.duration._

abstract class SelfScheduledActor(val initialDelay: FiniteDuration, val interval: FiniteDuration)
  (implicit val timeout: Timeout) extends Actor with ActorLogging {

  import context._

  private val timer = context.system.scheduler.schedule(initialDelay, interval, self, Tick)

  final override def receive: Receive = {
    val tick: Receive = {case Tick => onTick()}
    tick.orElse(recieveNonTick)
  }

  final override def postStop(): Unit = {
    timer.cancel()
  }

  def recieveNonTick: Receive
  def onTick(): Unit
}

object SelfScheduledActor {
  case object Tick
}
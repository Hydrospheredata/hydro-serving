package io.prototypes.ml_repository.datasource

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.actor.{ActorRef, Props}
import akka.util.Timeout
import awscala.s3._
import awscala.sqs._
import io.prototypes.ml_repository.Messages
import io.prototypes.ml_repository.datasource.SourceWatcher

import scala.concurrent.duration._

/**
  * Created by Bulat on 31.05.2017.
  */
trait SourceWatcher extends Actor with ActorLogging {
  import context._
  implicit private val timeout = Timeout(10.seconds)
  private val tick = context.system.scheduler.schedule(0.seconds, 500.millis, self, Messages.Watcher.Tick)

  def childPostStop(): Unit = {}

  final override def postStop(): Unit = {
    tick.cancel()
    childPostStop()
  }

}

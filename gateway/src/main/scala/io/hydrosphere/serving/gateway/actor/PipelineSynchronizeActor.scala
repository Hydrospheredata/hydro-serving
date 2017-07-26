package io.hydrosphere.serving.gateway.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import io.hydrosphere.serving.gateway.connector.ManagerConnector

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  */
class PipelineSynchronizeActor(manager: ManagerConnector, serveActor: ActorRef) extends Actor with ActorLogging {

  val Tick = "tick"

  override def preStart(): Unit = {
    context.system.scheduler.schedule(
      0 milliseconds,
      5000 milliseconds,
      context.self,
      Tick)
  }

  override def receive: Receive = {
    case Tick => manager.getEndpoints onComplete {
      case Success(result) =>
        val indexedEndpoints = Map(result map { a => a.name -> a }: _*)
        serveActor.tell(UpdateEndpoints(indexedEndpoints), context.self)
      case Failure(ex) => log.error(ex.getMessage, ex)
    }
  }
}

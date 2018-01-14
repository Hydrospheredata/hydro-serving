package io.hydrosphere.serving.manager.service.envoy.xds

import akka.actor.{Actor, ActorLogging}
import envoy.api.v2._
import io.grpc.stub.StreamObserver

import scala.collection.mutable
import scala.util.{Failure, Try}

case class SubscribeMsg(
  node: Node,
  resources: Seq[String],
  responseObserver: StreamObserver[DiscoveryResponse]
)

case class UnsubscribeMsg(
  responseObserver: StreamObserver[DiscoveryResponse]
)

abstract class AbstractDSActor extends Actor with ActorLogging {
  protected val observers = new mutable.HashSet[StreamObserver[DiscoveryResponse]]()

  protected def send(discoveryResponse: DiscoveryResponse, stream: StreamObserver[DiscoveryResponse]): Unit = {
    val t = Try(stream.onNext(discoveryResponse))
    t match {
      case Failure(e) =>
        log.error(s"Can't send message to $stream", e)
        observers -= stream
      case _ =>
    }
  }
}

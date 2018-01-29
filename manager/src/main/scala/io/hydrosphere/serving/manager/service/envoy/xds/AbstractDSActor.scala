package io.hydrosphere.serving.manager.service.envoy.xds

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorLogging}
import com.trueaccord.scalapb.{GeneratedMessage, Message}
import envoy.api.v2._
import io.grpc.stub.StreamObserver

import scala.collection.mutable
import scala.util.{Failure, Try}

case class SubscribeMsg(
  discoveryRequest: DiscoveryRequest,
  responseObserver: StreamObserver[DiscoveryResponse]
)

case class UnsubscribeMsg(
  responseObserver: StreamObserver[DiscoveryResponse]
)

abstract class AbstractDSActor[A <: GeneratedMessage with Message[A]](val typeUrl: String) extends Actor with ActorLogging {

  protected val ROUTE_CONFIG_NAME="mesh"

  private val observerNode = mutable.Map[StreamObserver[DiscoveryResponse], Node]()

  private val version = new AtomicLong(1L)

  private def needToExecute(v: DiscoveryRequest): Boolean =
    !v.versionInfo.contains(version.toString)

  protected def send(discoveryResponse: DiscoveryResponse, stream: StreamObserver[DiscoveryResponse]): Unit = {
    val t = Try(stream.onNext(discoveryResponse))
    t match {
      case Failure(e) =>
        log.error(s"Can't send message to $stream", e)
        observerNode.remove(stream)
      case _ =>
    }
  }

  protected def getObserverNode(responseObserver: StreamObserver[DiscoveryResponse]): Option[Node] =
    observerNode.get(responseObserver)

  private def sendToObserver(responseObserver: StreamObserver[DiscoveryResponse]) = {
    send(DiscoveryResponse(
      typeUrl = typeUrl,
      versionInfo = version.toString,
      nonce = version.toString,
      resources = formResources(responseObserver).map(s => com.google.protobuf.any.Any.pack(s))
    ), responseObserver)
  }

  private def increaseVersion() = {
    version.incrementAndGet()
    observerNode.keys.foreach(o => sendToObserver(o))
  }

  override def receive: Receive = {
    case subscribe: SubscribeMsg =>
      observerNode.put(subscribe.responseObserver, subscribe.discoveryRequest.node.getOrElse(Node.defaultInstance))
      if (needToExecute(subscribe.discoveryRequest)) {
        sendToObserver(subscribe.responseObserver)
      }

    case unsubcribe: UnsubscribeMsg =>
      observerNode.remove(unsubcribe.responseObserver)

    case x =>
      if (receiveStoreChangeEvents(x)) {
        increaseVersion()
      }
  }

  protected def receiveStoreChangeEvents(mes: Any): Boolean = false

  protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[A]
}

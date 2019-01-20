package io.hydrosphere.serving.manager.infrastructure.envoy.xds

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorLogging}
import envoy.api.v2._
import envoy.api.v2.core.Node
import io.grpc.stub.StreamObserver
import scalapb.{GeneratedMessage, Message}

import scala.collection.mutable
import scala.util.Try

case class SubscribeMsg(
  discoveryRequest: DiscoveryRequest,
  responseObserver: StreamObserver[DiscoveryResponse]
)

case class UnsubscribeMsg(
  responseObserver: StreamObserver[DiscoveryResponse]
)

abstract class AbstractDSActor[A <: GeneratedMessage with Message[A]](val typeUrl: String) extends Actor with ActorLogging {

  protected val ROUTE_CONFIG_NAME = "mesh"

  private val observerNode = mutable.Map[StreamObserver[DiscoveryResponse], Node]()
  private val observerResources = mutable.Map[StreamObserver[DiscoveryResponse], Set[String]]()

  private val version = new AtomicLong(System.currentTimeMillis())
  private val sentRequest = new AtomicLong(1)

  protected def send(discoveryResponse: DiscoveryResponse, stream: StreamObserver[DiscoveryResponse]): Unit = {
    val t = Try {
      stream.synchronized {
        //TODO quick fix - wrong fix, need to refactor ...DSActors
        log.debug(s"DiscoveryResponse: $discoveryResponse")

        stream.onNext(discoveryResponse)
      }
    } recover {
      case e =>
        log.error("Can't send message to {}, error {}:", stream, e)
        observerNode.remove(stream)
        observerResources.remove(stream)
        //TODO am I right? could I invoke onClose after error in onNext?
        stream.synchronized {
          stream.onError(e)
        }
    } get
  }

  protected def getObserverNode(responseObserver: StreamObserver[DiscoveryResponse]): Option[Node] =
    observerNode.get(responseObserver)

  private def sendToObserver(responseObserver: StreamObserver[DiscoveryResponse]) = {
    send(DiscoveryResponse(
      typeUrl = typeUrl,
      versionInfo = version.toString,
      nonce = sentRequest.getAndIncrement().toString,
      resources = formResources(responseObserver)
        .map(s => Try(com.google.protobuf.any.Any.pack(s)))
        .filter(_.isSuccess)
        .map(_.get)
    ), responseObserver)
  }

  private def increaseVersion() = {
    version.incrementAndGet()
    observerNode.keys.foreach(o => sendToObserver(o))
  }

  override def receive: Receive = {
    case subscribe: SubscribeMsg =>
      observerNode.put(subscribe.responseObserver, subscribe.discoveryRequest.node.getOrElse(Node.defaultInstance))

      val needUpdateResource = observerResources.get(subscribe.responseObserver) match {
        case Some(x) =>
          x != subscribe.discoveryRequest.resourceNames.toSet
        case None =>
          true
      }
      if (needUpdateResource) {
        observerResources.put(subscribe.responseObserver, subscribe.discoveryRequest.resourceNames.toSet)
      }
      val differentVersion = {
        version.toString != subscribe.discoveryRequest.versionInfo
      }

      if (differentVersion || needUpdateResource) {
        sendToObserver(subscribe.responseObserver)
      } else {
        send(DiscoveryResponse(
          typeUrl = "type.googleapis.com/com.google.protobuf.Empty",
          versionInfo = version.toString,
          nonce = sentRequest.get().toString
        ), subscribe.responseObserver)
      }

    case unsubcribe: UnsubscribeMsg =>
      observerNode.remove(unsubcribe.responseObserver)
      observerResources.remove(unsubcribe.responseObserver)

    case x =>
      if (receiveStoreChangeEvents(x)) {
        increaseVersion()
      }
  }

  protected def receiveStoreChangeEvents(mes: Any): Boolean = false

  protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[A]
}

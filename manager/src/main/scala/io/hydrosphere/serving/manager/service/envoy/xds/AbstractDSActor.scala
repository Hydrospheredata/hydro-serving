package io.hydrosphere.serving.manager.service.envoy.xds

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorLogging}
import scalapb.{GeneratedMessage, Message}
import envoy.api.v2._
import envoy.api.v2.core.Node
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

  protected val ROUTE_CONFIG_NAME = "mesh"

  private val observerNode = mutable.Map[StreamObserver[DiscoveryResponse], Node]()
  private val observerResources = mutable.Map[StreamObserver[DiscoveryResponse], Set[String]]()

  private val version = new AtomicLong(System.currentTimeMillis())
  private val sentRequest = new AtomicLong(1)

  protected def send(discoveryResponse: DiscoveryResponse, stream: StreamObserver[DiscoveryResponse]): Unit = {
    val t = Try({
      stream.synchronized{
        //TODO quick fix - wrong fix, need to refactor ...DSActors
        log.debug(s"DiscoveryResponse: $discoveryResponse")

        stream.onNext(discoveryResponse)
      }
    })
    t match {
      case Failure(e) =>
        log.error("Can't send message to {}, error {}:", stream, e)
        observerNode.remove(stream)
        observerResources.remove(stream)
        //TODO am I right? could I invoke onClose after error in onNext?
        stream.synchronized{
          stream.onError(e)
        }
      case _ =>
    }
  }

  protected def getObserverNode(responseObserver: StreamObserver[DiscoveryResponse]): Option[Node] =
    observerNode.get(responseObserver)

  private def sendToObserver(responseObserver: StreamObserver[DiscoveryResponse]) = {
    send(DiscoveryResponse(
      typeUrl = typeUrl,
      versionInfo = version.toString,
      nonce = sentRequest.getAndIncrement().toString,
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
        !subscribe.discoveryRequest.versionInfo.contains(version.toString)
      }

      if (differentVersion || needUpdateResource) {
        sendToObserver(subscribe.responseObserver)
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

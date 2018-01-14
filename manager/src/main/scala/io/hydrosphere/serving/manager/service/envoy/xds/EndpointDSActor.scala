package io.hydrosphere.serving.manager.service.envoy.xds

import envoy.api.v2.SocketAddress.PortSpecifier
import envoy.api.v2._
import io.grpc.stub.StreamObserver

import scala.collection.mutable

case class ClusterEndpoint(
  host: String,
  port: Int
)

case class ClusterInfo(
  name: String,
  endpoints: Seq[ClusterEndpoint]
)

case class RenewEndpoints(
  clusters: Seq[ClusterInfo]
)

class EndpointDSActor extends AbstractDSActor {
  private val endpoints = mutable.Map[String, ClusterLoadAssignment]()

  private val observerNode = mutable.Map[StreamObserver[DiscoveryResponse], Node]()

  private def renewEndpoints(r: RenewEndpoints): Unit = {
    val clusters = r.clusters.map(d => d.name).toSet
    val toRemove = clusters -- endpoints.keySet
    toRemove.foreach(d => endpoints.remove(d))

    r.clusters.foreach(cluster => {
      val cl = createCluster(cluster)
      endpoints.put(cl.clusterName, cl)
    })
  }

  private def createCluster(cluster: ClusterInfo): ClusterLoadAssignment = ClusterLoadAssignment(
    endpoints = Seq(LocalityLbEndpoints(
      lbEndpoints = cluster.endpoints.map(endpoint => {
        LbEndpoint(
          endpoint = Some(Endpoint(
            address = Some(Address(
              address = Address.Address.SocketAddress(
                SocketAddress(
                  address = endpoint.host,
                  portSpecifier = PortSpecifier.PortValue(endpoint.port)
                )
              )
            ))
          ))
        )
      })
    ))
  )

  private val mainApplicationEndpoint: ClusterLoadAssignment = ClusterLoadAssignment(
    endpoints = Seq(LocalityLbEndpoints(
      lbEndpoints = Seq(LbEndpoint(
        endpoint = Some(Endpoint(
          address = Some(Address(
            address = Address.Address.SocketAddress(
              SocketAddress(
                address = "192.168.90.68",
                portSpecifier = PortSpecifier.PortValue(9090)
              )
            )
          ))
        ))
      ))
    ))
  )

  private def sendEndpoints(responseObserver: StreamObserver[DiscoveryResponse]): Unit = {
    observerNode.get(responseObserver).foreach(node => {
      val toSend = endpoints.values.map(e => {
        mainApplicationEndpoint.withClusterName(node.id)
        /*if (e.clusterName == node.id) {
          mainApplicationEndpoint.withClusterName(node.id)
        } else {
          e
        }*/
      })

      send(DiscoveryResponse(
        typeUrl = "type.googleapis.com/envoy.api.v2.ClusterLoadAssignment",
        versionInfo = "0",
        resources = toSend.map(s => com.google.protobuf.any.Any.pack(s)).toSeq
      ), responseObserver)
    })
  }

  private def sendEndpoints(): Unit = observers.foreach(o => sendEndpoints(o))

  override def receive: Receive = {
    case subscribe: SubscribeMsg =>
      observers += subscribe.responseObserver
      observerNode.put(subscribe.responseObserver, subscribe.node)
      sendEndpoints(subscribe.responseObserver)

    case unsubcribe: UnsubscribeMsg =>
      observers -= unsubcribe.responseObserver
      observerNode.remove(unsubcribe.responseObserver)

    case r: RenewEndpoints =>
      renewEndpoints(r)
      sendEndpoints()
  }
}

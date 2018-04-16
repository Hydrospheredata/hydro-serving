package io.hydrosphere.serving.manager.service.envoy.xds

import envoy.api.v2.SocketAddress.PortSpecifier
import envoy.api.v2._
import io.grpc.stub.StreamObserver

import scala.collection.mutable

case class ClusterEndpoint(
    advertisedHost: String,
    advertisedPort: Int,
    host: String,
    port: Int
)

case class ClusterInfo(
    name: String,
    endpoints: Set[ClusterEndpoint]
)

case class RenewEndpoints(
    clusters: Seq[ClusterInfo]
)

case class AddEndpoints(
    clusters: Seq[ClusterInfo]
)

case class RemoveEndpoints(
    names: Set[String]
)

class EndpointDSActor extends AbstractDSActor[ClusterLoadAssignment](
  typeUrl = "type.googleapis.com/envoy.api.v2.ClusterLoadAssignment"
) {

  private val clusterEndpoints = mutable.Map[String, ClusterInfo]()

  override protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[ClusterLoadAssignment] =
    getObserverNode(responseObserver).map(node => {
      clusterEndpoints.values.map(e => {
        createCluster(e, node)
      })
    }).getOrElse(Seq()).toSeq


  override def receiveStoreChangeEvents(mes: Any): Boolean =
    mes match {
      case r: RenewEndpoints =>
        renewEndpoints(r.clusters)
      case d: RemoveEndpoints =>
        removeClusters(d.names).contains(true)
      case d: AddEndpoints =>
        createOrUpdate(d.clusters).contains(true)
      case _ => false
    }


  private def renewEndpoints(clusters: Seq[ClusterInfo]): Boolean = {
    val toRemove = clusters.map(p => p.name).toSet -- clusterEndpoints.keySet
    val r = removeClusters(toRemove) ++ createOrUpdate(clusters)
    r.contains(true)
  }

  private def removeClusters(toRemove: Set[String]): Set[Boolean] =
    toRemove.map(r => {
      clusterEndpoints.remove(r).isDefined
    })


  private def createOrUpdate(clusters: Seq[ClusterInfo]): Set[Boolean] = {
    val res = clusters.map(p => {
      val currentEndpoints = clusterEndpoints.get(p.name).map(_.endpoints).getOrElse(Set())
      if (currentEndpoints.isEmpty || (currentEndpoints &~ p.endpoints).nonEmpty) {
        clusterEndpoints.put(p.name, p)
        true
      } else {
        false
      }
    }).toSet
    res
  }

  private def createCluster(cluster: ClusterInfo, node: Node): ClusterLoadAssignment = ClusterLoadAssignment(
    clusterName = cluster.name,
    endpoints = Seq(LocalityLbEndpoints(
      lbEndpoints = cluster.endpoints.map(endpoint => {
        LbEndpoint(
          endpoint = Some(Endpoint(
            address = Some(Address(
              address = Address.Address.SocketAddress(
                node.metadata
                  .flatMap(_.fields.get("hostPrivateIp"))
                  .filter(_.kind.isStringValue)
                  .filter(_.getStringValue == endpoint.host)
                  .map(_ => {
                    SocketAddress(
                      address = endpoint.host,
                      portSpecifier = PortSpecifier.PortValue(endpoint.port)
                    )
                  })
                  .getOrElse(SocketAddress(
                    address = endpoint.advertisedHost,
                    portSpecifier = PortSpecifier.PortValue(endpoint.advertisedPort)
                  ))
              )
            ))
          ))
        )
      }).toSeq
    ))
  )
}

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

class EndpointDSActor(
  val specialCluster: Boolean = false,
  val specialHost: String = "mainapplication",
  val specialPort: Int = 9091
) extends AbstractDSActor[ClusterLoadAssignment](
  typeUrl = "type.googleapis.com/envoy.api.v2.ClusterLoadAssignment"
) {

  private val endpoints = mutable.Map[String, ClusterLoadAssignment]()

  private val clusterEndpoints = mutable.Map[String, Set[ClusterEndpoint]]()

  override protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[ClusterLoadAssignment] =
    getObserverNode(responseObserver).map(node => {
      endpoints.values.map(e => {
        if (specialCluster && e.clusterName == node.id) {
          mainApplicationEndpoint.withClusterName(node.id)
        } else {
          e
        }
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
    val toRemove = clusters.map(p => p.name).toSet -- endpoints.keySet
    val r = removeClusters(toRemove) ++ createOrUpdate(clusters)
    r.contains(true)
  }

  private def removeClusters(toRemove: Set[String]): Set[Boolean] =
    toRemove.map(r => {
      clusterEndpoints.remove(r)
        .map(_ => endpoints.remove(r)).isDefined
    })


  private def createOrUpdate(clusters: Seq[ClusterInfo]): Set[Boolean] = {
    val res=clusters.map(p => {
      val currentEndpoints = clusterEndpoints.get(p.name)
      if (currentEndpoints.isEmpty || (currentEndpoints.get &~ p.endpoints).nonEmpty) {
        val cl = createCluster(p)
        endpoints.put(p.name, cl)
        clusterEndpoints.put(p.name, p.endpoints)
        true
      } else {
        false
      }
    }).toSet
    res
  }

  private def createCluster(cluster: ClusterInfo): ClusterLoadAssignment = ClusterLoadAssignment(
    clusterName = cluster.name,
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
      }).toSeq
    ))
  )

  private val mainApplicationEndpoint: ClusterLoadAssignment = ClusterLoadAssignment(
    endpoints = Seq(LocalityLbEndpoints(
      lbEndpoints = Seq(LbEndpoint(
        endpoint = Some(Endpoint(
          address = Some(Address(
            address = Address.Address.SocketAddress(
              SocketAddress(
                address = specialHost,
                portSpecifier = PortSpecifier.PortValue(specialPort)
              )
            )
          ))
        ))
      ))
    ))
  )
}

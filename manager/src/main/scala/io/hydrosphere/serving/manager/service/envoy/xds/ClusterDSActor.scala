package io.hydrosphere.serving.manager.service.envoy.xds

import com.google.protobuf.duration.Duration
import envoy.api.v2.Cluster.EdsClusterConfig
import envoy.api.v2.ConfigSource.ConfigSourceSpecifier
import envoy.api.v2.{AggregatedConfigSource, Cluster, ConfigSource, DiscoveryResponse}
import io.grpc.stub.StreamObserver

import scala.collection.mutable

case class ClusterAdded(names: Set[String])

case class ClusterRemoved(names: Set[String])

case class SyncCluster(names: Set[String])

class ClusterDSActor extends AbstractDSActor[Cluster](typeUrl = "type.googleapis.com/envoy.api.v2.Cluster") {

  private val clusters = new mutable.ListBuffer[Cluster]()

  private val clustersNames = new mutable.HashSet[String]()

  private def addClusters(names: Set[String]): Set[Boolean] =
    names.map(name => {
      if (clustersNames.add(name)) {
        clusters += Cluster(
          name = name,
          `type` = Cluster.DiscoveryType.EDS,
          connectTimeout = Some(Duration(seconds = 0, nanos = 25000000)),
          edsClusterConfig = Some(
            EdsClusterConfig(
              edsConfig = Some(ConfigSource(
                configSourceSpecifier = ConfigSourceSpecifier.Ads(
                  AggregatedConfigSource()
                ))
              )
            )
          )
        )
        true
      } else {
        false
      }
    })

  private def removeClusters(names: Set[String]): Set[Boolean] =
    names.map(name => {
      if (clustersNames.remove(name)) {
        clusters --= clusters.filter(c => !clustersNames.contains(c.name))
        true
      } else {
        false
      }
    })


  private def syncClusters(names: Set[String]): Set[Boolean] = {
    val toRemove = clustersNames.toSet -- names
    val toAdd = names -- clustersNames

    removeClusters(toRemove) ++ addClusters(toAdd)
  }

  override def receiveStoreChangeEvents(mes: Any): Boolean = {
    val results = mes match {
      case ClusterAdded(names) =>
        addClusters(names)
      case ClusterRemoved(names) =>
        removeClusters(names)
      case SyncCluster(names) =>
        removeClusters(names)
    }
    results.contains(true)
  }

  override protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[Cluster] =
    clusters
}
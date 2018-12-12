package io.hydrosphere.serving.manager.infrastructure.envoy.xds

import com.google.protobuf.duration.Duration
import envoy.api.v2.Cluster.EdsClusterConfig
import envoy.api.v2._
import envoy.api.v2.core.{AggregatedConfigSource, ConfigSource, Http2ProtocolOptions}
import envoy.api.v2.core.ConfigSource.ConfigSourceSpecifier
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.domain.clouddriver.DefaultConstants._

import scala.collection.mutable

case class AddCluster(names: Set[String])

case class RemoveClusters(names: Set[String])

case class SyncCluster(names: Set[String])

class ClusterDSActor extends AbstractDSActor[Cluster](typeUrl = "type.googleapis.com/envoy.api.v2.Cluster") {

  private val clusters = new mutable.ListBuffer[Cluster]()

  private val clustersNames = new mutable.HashSet[String]()

  private val httClusters = Set(
    MANAGER_HTTP_NAME,
    MANAGER_UI_NAME,
    MONITORING_HTTP_NAME,
    PROFILER_HTTP_NAME,
    GATEWAY_HTTP_NAME
  )

  private def createCluster(name: String): Cluster = {
    val res = Cluster(
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

    if (httClusters.contains(name)) {
      res
    } else {
      res.withHttp2ProtocolOptions(Http2ProtocolOptions())
    }
  }

  private def addClusters(names: Set[String]): Set[Boolean] = {
    log.debug(names.toString())
    names.map { name =>
      if (clustersNames.add(name)) {
        clusters += createCluster(name)
        true
      } else {
        false
      }
    }
  }

  private def removeClusters(names: Set[String]): Set[Boolean] = {
    log.debug(names.toString())
    names.map { name =>
      if (clustersNames.remove(name)) {
        clusters --= clusters.filter(c => !clustersNames.contains(c.name))
        true
      } else {
        false
      }
    }
  }

  private def syncClusters(names: Set[String]): Set[Boolean] = {
    log.debug(names.toString())
    val toRemove = clustersNames.toSet -- names
    val toAdd = names -- clustersNames

    removeClusters(toRemove) ++ addClusters(toAdd)
  }

  override def receiveStoreChangeEvents(mes: Any): Boolean = {
    log.debug("Got message: {}", mes)
    val results = mes match {
      case AddCluster(names) =>
        addClusters(names)
      case RemoveClusters(names) =>
        removeClusters(names)
      case SyncCluster(names) =>
        syncClusters(names)
    }
    results.contains(true)
  }

  override protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[Cluster] =
    clusters

}
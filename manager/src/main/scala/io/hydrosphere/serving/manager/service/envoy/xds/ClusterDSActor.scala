package io.hydrosphere.serving.manager.service.envoy.xds

import com.google.protobuf.duration.Duration
import envoy.api.v2.Cluster.EdsClusterConfig
import envoy.api.v2._
import envoy.api.v2.core.{AggregatedConfigSource, ConfigSource, Http2ProtocolOptions}
import envoy.api.v2.core.ConfigSource.ConfigSourceSpecifier
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.service.clouddriver.CloudDriverService

import scala.collection.mutable

case class AddCluster(names: Set[String])

case class RemoveClusters(names: Set[String])

case class SyncCluster(names: Set[String])

class ClusterDSActor extends AbstractDSActor[Cluster](typeUrl = "type.googleapis.com/envoy.api.v2.Cluster") {

  private val clusters = new mutable.ListBuffer[Cluster]()

  private val clustersNames = new mutable.HashSet[String]()

  private val httClusters = Set(
    CloudDriverService.MANAGER_HTTP_NAME,
    CloudDriverService.MANAGER_UI_NAME,
    CloudDriverService.MONITORING_HTTP_NAME,
    CloudDriverService.PROFILER_HTTP_NAME,
    CloudDriverService.GATEWAY_HTTP_NAME
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
      case x @ AddCluster(names) =>
        addClusters(names)
      case x @ RemoveClusters(names) =>
        removeClusters(names)
      case x @ SyncCluster(names) =>
        syncClusters(names)
    }
    results.contains(true)
  }

  override protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[Cluster] =
    clusters

}
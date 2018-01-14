package io.hydrosphere.serving.manager.service.envoy.xds

import com.google.protobuf.duration.Duration
import envoy.api.v2.Cluster.EdsClusterConfig
import envoy.api.v2.ConfigSource.ConfigSourceSpecifier
import envoy.api.v2.{AggregatedConfigSource, Cluster, ConfigSource, DiscoveryResponse}

import scala.collection.mutable.ListBuffer

case class ClusterAdded(names: Seq[String])

case class ClusterRemoved(names: Seq[String])

class ClusterDSActor extends AbstractDSActor {

  private val clusters = new ListBuffer[Cluster]()

  override def receive: Receive = {
    case subscribe: SubscribeMsg =>
      observers += subscribe.responseObserver
      send(createDiscoveryResponse(), subscribe.responseObserver)

    case unsubcribe: UnsubscribeMsg =>
      observers -= unsubcribe.responseObserver

    case add: ClusterAdded =>

      add.names.foreach(name => {
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
      })

      sendClusters()

    case rem: ClusterRemoved =>
      rem.names.foreach(name => {
        clusters --= clusters.filter(c => c.name == name)
      })
      sendClusters()

  }

  private def createDiscoveryResponse(): DiscoveryResponse = {
    DiscoveryResponse(
      typeUrl = "type.googleapis.com/envoy.api.v2.Cluster",
      versionInfo = "0",
      resources = clusters.map(s => com.google.protobuf.any.Any.pack(s))
    )
  }

  private def sendClusters(): Unit = {
    val discoveryResponse = createDiscoveryResponse()
    observers.foreach(s => {
      send(discoveryResponse, s)
    })
  }
}
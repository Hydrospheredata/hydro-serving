package io.hydrosphere.serving.manager.infrastructure.envoy.xds

import envoy.api.v2.DiscoveryResponse
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.domain.application.Application
import io.hydrosphere.serving.manager.grpc.applications.{Application => GApp}
import io.hydrosphere.serving.manager.infrastructure.envoy.events.DiscoveryEvent._
import io.hydrosphere.serving.manager.infrastructure.envoy.Converters

import scala.collection.mutable

class ApplicationDSActor extends AbstractDSActor[GApp](typeUrl = "type.googleapis.com/io.hydrosphere.serving.manager.grpc.applications.Application") {

  private val applications = mutable.Map[Long, GApp]()

  override def receiveStoreChangeEvents(mes: Any): Boolean =
    mes match {
      case a: SyncApplications =>
        applications.clear()
        addOrUpdateApplications(a.applications)
        true
      case a: ApplicationChanged =>
        addOrUpdateApplications(Seq(a.application))
        true
      case a: ApplicationRemoved =>
        removeApplications(Set(a.application.id))
          .contains(true)
      case _ => false
    }

  private def addOrUpdateApplications(apps: Seq[Application]): Unit =
    apps
      .map(Converters.grpcApp)
      .foreach { a =>
        applications.put(a.id, a)
      }

  private def removeApplications(ids: Set[Long]): Set[Boolean] =
    ids.map(id => applications.remove(id).nonEmpty)

  override protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[GApp] =
    applications.values.toSeq
}
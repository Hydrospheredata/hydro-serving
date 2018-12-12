package io.hydrosphere.serving.manager.domain.clouddriver

import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.service.Service


trait CloudDriverAlgebra[F[_]] {

  def getMetricServiceTargets: F[Seq[MetricServiceTargets]]

  def serviceList(): F[Seq[CloudService]]

  def deployService(
    service: Service,
    runtime: DockerImage,
    modelVersion: DockerImage,
    hostSelector: Option[HostSelector]
  ): F[CloudService]

  def services(serviceIds: Set[Long]): F[Seq[CloudService]]

  def removeService(serviceId: Long): F[Unit]
}
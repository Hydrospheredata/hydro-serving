package io.hydrosphere.serving.manager.config

import com.amazonaws.regions.Regions

sealed trait CloudDriverConfiguration {
  def loggingConfiguration: Option[ModelLoggingConfiguration]
}

object CloudDriverConfiguration {
  case class Swarm(
    networkName: String,
    loggingConfiguration: Option[ModelLoggingConfiguration]
  ) extends CloudDriverConfiguration

  case class Docker(
    networkName: String,
    loggingConfiguration: Option[ModelLoggingConfiguration]
  ) extends CloudDriverConfiguration

  case class Local(
    loggingConfiguration: Option[ModelLoggingConfiguration],
    monitoring: Option[LocalDockerCloudDriverServiceConfiguration],
    profiler: Option[LocalDockerCloudDriverServiceConfiguration],
    gateway: Option[LocalDockerCloudDriverServiceConfiguration]
  ) extends CloudDriverConfiguration

  case class Ecs(
    region: Regions,
    cluster: String,
    accountId: String,
    loggingConfiguration: Option[ModelLoggingConfiguration],
    memoryReservation: Int = 200,
    internalDomainName: String,
    vpcId: String
  ) extends CloudDriverConfiguration
}


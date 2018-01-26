package io.hydrosphere.serving.manager.configuration

import com.amazonaws.regions.Regions


trait CloudDriverConfiguration

object CloudDriverConfiguration {
  case class LocalDockerCloudDriverConfiguration()
    extends CloudDriverConfiguration

  case class DockerCloudDriverConfiguration(
    networkName: String,
    loggingGelfHost: Option[String]
  ) extends CloudDriverConfiguration

  case class ECSCloudDriverConfiguration(
    region: Regions,
    cluster: String,
    accountId: String,
    loggingGelfHost: Option[String]
  ) extends CloudDriverConfiguration

  case class SwarmCloudDriverConfiguration(
    networkName: String
  ) extends CloudDriverConfiguration

}

package io.hydrosphere.serving.manager.configuration

import com.amazonaws.regions.Regions


trait DockerRepositoryConfiguration

object DockerRepositoryConfiguration {
  case class ECSDockerRepositoryConfiguration(
    region: Regions,
    accountId: String
  ) extends DockerRepositoryConfiguration

  case class LocalDockerRepositoryConfiguration() extends DockerRepositoryConfiguration

}
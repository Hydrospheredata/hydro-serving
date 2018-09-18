package io.hydrosphere.serving.manager.config

import com.amazonaws.regions.Regions

sealed trait DockerRepositoryConfiguration

object DockerRepositoryConfiguration {
  case object Local extends DockerRepositoryConfiguration

  case class Ecs(
    region: Regions,
    accountId: String
  ) extends DockerRepositoryConfiguration
  
  case class Remote(
    host: String,
    username: Option[String],
    password: Option[String],
    pullHost: Option[String]
  ) extends  DockerRepositoryConfiguration

}

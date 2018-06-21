package io.hydrosphere.serving.manager.util.docker

case class DockerRegistryAuth(
  username: Option[String],
  password: Option[String],
  email: Option[String],
  serverAddress: Option[String],
  identityToken: Option[String],
  auth: Option[String]
)

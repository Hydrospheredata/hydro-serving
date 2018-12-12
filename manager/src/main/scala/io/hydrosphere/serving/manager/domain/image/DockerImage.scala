package io.hydrosphere.serving.manager.domain.image

case class DockerImage(
  name: String,
  tag: String,
  sha256: Option[String] = None
) {
  def fullName: String = name + ":" + tag
}
package io.hydrosphere.serving.manager.domain.image

trait ImageRepository[F[_]] {
  def push(dockerImage: DockerImage): F[Unit]

  def getImage(name: String, tag: String): DockerImage
}

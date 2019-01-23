package io.hydrosphere.serving.manager.infrastructure.image.repositories

import io.hydrosphere.serving.manager.domain.image.{DockerImage, ImageRepository}

import scala.concurrent.Future

class LocalImageRepository extends ImageRepository[Future] {
  override def getImage(name: String, tag: String): DockerImage = {
    DockerImage(
      name = name,
      tag = tag
    )
  }

  override def push(dockerImage: DockerImage): Future[Unit] =
    Future.successful(())
}
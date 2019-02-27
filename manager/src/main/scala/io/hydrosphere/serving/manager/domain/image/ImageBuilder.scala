package io.hydrosphere.serving.manager.domain.image

import java.nio.file.Path

trait ImageBuilder[F[_]] {
  def build(
    buildPath: Path,
    image: DockerImage
  ): F[String]
}

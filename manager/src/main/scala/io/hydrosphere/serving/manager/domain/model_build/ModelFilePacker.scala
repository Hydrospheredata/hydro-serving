package io.hydrosphere.serving.manager.domain.model_build

import java.nio.file.Path

import io.hydrosphere.serving.manager.domain.model_version.BuildRequest

trait ModelFilePacker[F[_]] {
  def pack(modelVersion: BuildRequest): F[Path]
}
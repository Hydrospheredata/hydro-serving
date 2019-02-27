package io.hydrosphere.serving.manager.domain.model_build

import cats.effect.concurrent.Deferred
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion

case class BuildResult[F[_]](startedVersion: ModelVersion, completedVersion: Deferred[F, ModelVersion])

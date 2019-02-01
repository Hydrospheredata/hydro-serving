package io.hydrosphere.serving.manager.domain.model_version

import cats.effect.concurrent.Deferred

case class BuildResult[F[_]](startedVersion: ModelVersion, completedVersion: Deferred[F, ModelVersion])

package io.hydrosphere.serving.manager.domain.application

import cats.effect.concurrent.Deferred

case class ApplicationBuildResult[F[_]](
  started: Application,
  completed: Deferred[F, Application]
)
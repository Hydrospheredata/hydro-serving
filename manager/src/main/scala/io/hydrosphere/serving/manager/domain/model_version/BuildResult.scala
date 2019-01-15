package io.hydrosphere.serving.manager.domain.model_version

import scala.concurrent.Future

case class BuildResult(startedVersion: ModelVersion, completedVersion: Future[ModelVersion])

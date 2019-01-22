package io.hydrosphere.serving.manager.domain.application

import scala.concurrent.Future

case class ApplicationBuildResult(started: Application, completed: Future[Application])
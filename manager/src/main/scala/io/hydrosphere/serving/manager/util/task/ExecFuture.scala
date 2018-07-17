package io.hydrosphere.serving.manager.util.task

import scala.concurrent.Future

case class ExecFuture[Req, Res](taskStatus: Future[ServiceTask[Req, Res]], future: Future[Res])

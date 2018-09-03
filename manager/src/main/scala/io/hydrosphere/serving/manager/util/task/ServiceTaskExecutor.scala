package io.hydrosphere.serving.manager.util.task

import io.hydrosphere.serving.model.api.{HFResult, HResult}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}



trait ServiceTaskExecutor[Req, Res] extends Logging {
  def executionContext: ExecutionContext

  def makeUpdater(task: Req): Future[ServiceTaskUpdater[Req, Res]]

  def execute(request: Req): ExecFuture[Req, Res]

  final def runRequestF(request: Req)
    (bodyF: (Req, ServiceTaskUpdater[Req, Res]) => Future[Res])
  : ExecFuture[Req, Res] = {
    implicit val ec = executionContext

    val updaterF = makeUpdater(request)
    val resultF = updaterF.flatMap { updater =>
      bodyF(request, updater)
    }(executionContext)
    resultF.failed.foreach { ex =>
      logger.warn(s" Service task failed: $ex")
    }(executionContext)
    ExecFuture[Req, Res](updaterF.flatMap(_.task)(executionContext), resultF)
  }

  final def runRequest(request: Req)
    (body: (Req, ServiceTaskUpdater[Req, Res]) => Res)
  : ExecFuture[Req, Res] = {
    implicit val ec = executionContext
    
    runRequestF(request) { (req, updater) =>
      Future(body(req, updater))(executionContext)
    }
  }
}
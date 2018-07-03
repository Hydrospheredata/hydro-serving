package io.hydrosphere.serving.manager.util.task

import java.time.LocalDateTime

import scala.concurrent.{ExecutionContext, Future}

trait ServiceTaskUpdater[Req, Res] {
  def task()(implicit ec: ExecutionContext): Future[ServiceTask[Req, Res]]

  def executor: ServiceTaskExecutor[Req, Res]

  final def running()(implicit ec: ExecutionContext): Future[ServiceTask[Req, Res]] = {
    task().map { originalTask =>
      val runningTask = originalTask.run()
      updateTaskStorage(runningTask)
      runningTask
    }
  }

  final def failed(message: String, time: LocalDateTime = LocalDateTime.now())(implicit ec: ExecutionContext): Future[ServiceTask[Req, Res]] = {
    task().map { originalTask =>
      val runningTask = originalTask.fail(message, time)
      updateTaskStorage(runningTask)
      runningTask
    }
  }

  final def finished(result: Res, time: LocalDateTime = LocalDateTime.now())(implicit ec: ExecutionContext): Future[ServiceTask[Req, Res]] = {
    task().map { originalTask =>
      val runningTask = originalTask.finish(result, time)
      updateTaskStorage(runningTask)
      runningTask
    }
  }

  protected def updateTaskStorage(task: ServiceTask[Req, Res])(implicit ec: ExecutionContext): Future[Unit]

  def log(log: String): Unit
}
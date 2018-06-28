package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.Executors

import io.hydrosphere.serving.manager.model.{HFResult, HResult}
import org.apache.logging.log4j.scala.Logging

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

case class ExecFuture[Req, Res](taskStatus: ServiceTask[Req, Res], future: Future[HResult[Res]])

class ServiceTaskExecutor[Req, Res] private (val executionContext: ExecutionContext) extends Logging {
  val taskInfos = TrieMap.empty[UUID, ServiceTask[Req, Res]]

  def runRequestF(request: Req)
    (bodyF: (Req, ServiceTaskUpdater[Req, Res], ExecutionContext) => HFResult[Res]) = {
    val startedAt = LocalDateTime.now()
    val id = UUID.randomUUID()
    taskInfos += id -> ServiceTask.create[Req, Res](id, startedAt, request)
    val updater = ServiceTaskUpdater(id, this)
    val future = bodyF(request, updater, executionContext)
    future.failed.foreach { ex =>
      logger.warn(s"[$id] Service task failed: $ex")
      taskInfos += id -> taskInfos(id).fail(ex.getMessage, LocalDateTime.now())
    }(executionContext)
    ExecFuture(updater.task, future)
  }

  def runRequest(request: Req)
    (body: (Req, ServiceTaskUpdater[Req, Res]) => HResult[Res])
  : ExecFuture[Req, Res] = {
    val startedAt = LocalDateTime.now()
    val id = UUID.randomUUID()
    taskInfos += id -> ServiceTask.create[Req, Res](id, startedAt, request)
    val updater = ServiceTaskUpdater(id, this)
    val future = Future(body(request, updater))(executionContext)
    future.failed.foreach { ex =>
      logger.warn(s"[$id] Service task failed: $ex")
      taskInfos += id -> taskInfos(id).fail(ex.getMessage, LocalDateTime.now())
    }(executionContext)
    ExecFuture(updater.task, future)
  }
}

object ServiceTaskExecutor {
  def withFixedPool[Req, Res](threads: Int) = {
    val executionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(threads))
    new ServiceTaskExecutor[Req, Res](executionContext)
  }

  def withExecutionContext[Req, Res](executionContext: ExecutionContext) = {
    new ServiceTaskExecutor[Req, Res](executionContext)
  }
}

case class ServiceTaskUpdater[Req, Res](taskId: UUID, executor: ServiceTaskExecutor[Req, Res]) {
  def running(): Unit = {
    executor.taskInfos += taskId -> task.run()
  }

  def failed(message: String, time: LocalDateTime = LocalDateTime.now()): Unit = {
    executor.taskInfos += taskId -> task.fail(message, LocalDateTime.now())
  }

  def finished(result: Res, time: LocalDateTime = LocalDateTime.now()): Unit = {
    executor.taskInfos += taskId -> task.finish(result, time)
  }

  def log(log: String): Unit = {
    executor.taskInfos += taskId -> task.log(log)
  }

  def task = executor.taskInfos(taskId)
}

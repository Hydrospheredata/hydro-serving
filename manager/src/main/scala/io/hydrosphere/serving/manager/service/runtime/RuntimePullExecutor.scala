package io.hydrosphere.serving.manager.service.runtime

import java.time.LocalDateTime

import com.amazonaws.services.ecr.model.ImageNotFoundException
import com.spotify.docker.client.exceptions.DockerException
import com.spotify.docker.client.messages.ProgressMessage
import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.{CreateRuntimeRequest, PullRuntime, Runtime}
import io.hydrosphere.serving.manager.repository.{RuntimePullRepository, RuntimeRepository}
import io.hydrosphere.serving.manager.util.task.{ExecFuture, ServiceTask, ServiceTaskExecutor, ServiceTaskUpdater}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RuntimePullExecutor(
  val pullRepository: RuntimePullRepository,
  val runtimeRepository: RuntimeRepository,
  val dockerClient: DockerClient,
  val executionContext: ExecutionContext
) extends ServiceTaskExecutor[CreateRuntimeRequest, Runtime] {
  implicit val lightEC = ExecutionContext.global

  override def makeUpdater(request: CreateRuntimeRequest): Future[ServiceTaskUpdater[CreateRuntimeRequest, Runtime]] = {
    val originalTask = ServiceTask.create[CreateRuntimeRequest, Runtime](0, LocalDateTime.now(), request)

    pullRepository.create(PullRuntime.fromServiceTask(originalTask)).map { actualTask =>
      RuntimePullUpdater(actualTask.id, this, pullRepository)
    }
  }

  override def execute(request: CreateRuntimeRequest): ExecFuture[CreateRuntimeRequest, Runtime] = {
    runRequestF(request)(pullDockerImage)
  }

  def pullDockerImage(request: CreateRuntimeRequest, updater: ServiceTaskUpdater[CreateRuntimeRequest, Runtime]) = {
    try {
      updater.running().flatMap { t =>
        val logHandler = new ProgressHandler {
          override def progress(message: ProgressMessage): Unit = updater.log(message.status())
        }
        logger.info(s"Start docker pull ${request.fullImage}")
        dockerClient.pull(request.fullImage, logHandler)
        val runtime = Runtime(
          id = 0,
          name = request.name,
          version = request.version,
          suitableModelType = request.modelTypes.map(ModelType.fromTag),
          tags = request.tags,
          configParams = request.configParams
        )
        for {
          newRuntime <- runtimeRepository.create(runtime)
          _ <- updater.finished(newRuntime)
        } yield {
          newRuntime
        }
      }
    } catch {
      case NonFatal(err) =>
        val newFailStatus = err match {
          case ex: ImageNotFoundException =>
            updater.failed(s"Couldn't find an image ${request.fullImage}")
            ex
          case ex: DockerException =>
            updater.failed(s"Internal docker error")
            ex
          case ex: Throwable =>
            updater.failed(s"Unexpected exception")
            ex
        }
        logger.warn(s"Docker pull failed: $newFailStatus")
        throw newFailStatus
    }
  }
}

case class RuntimePullUpdater(
  taskId: Long,
  executor: RuntimePullExecutor,
  runtimePullRepository: RuntimePullRepository) extends ServiceTaskUpdater[CreateRuntimeRequest, Runtime] with Logging{
  override def task()(implicit ec: ExecutionContext): Future[ServiceTask[CreateRuntimeRequest, Runtime]] = {
    runtimePullRepository.get(taskId).map(_.get.toServiceTask)
  }

  override protected def updateTaskStorage(task: ServiceTask[CreateRuntimeRequest, Runtime])(implicit ec: ExecutionContext): Future[Unit] = {
    runtimePullRepository.update(PullRuntime.fromServiceTask(task)).map(_ => Unit)
  }

  override def log(log: String): Unit = logger.info(s"Runtime pull $taskId: " + log)
}
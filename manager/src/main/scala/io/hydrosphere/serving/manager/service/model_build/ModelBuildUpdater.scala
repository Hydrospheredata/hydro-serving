package io.hydrosphere.serving.manager.service.model_build

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.db.{BuildRequest, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.util.task.{ServiceTask, ServiceTaskExecutor, ServiceTaskUpdater}

import scala.concurrent.{ExecutionContext, Future}

case class ModelBuildUpdater(
  taskId: Long,
  executor: ServiceTaskExecutor[BuildRequest, ModelVersion],
  modelBuildRepository: ModelBuildRepository
) extends ServiceTaskUpdater[BuildRequest, ModelVersion] {

  override def log(log: String): Unit = ???

  override def task()(implicit ec: ExecutionContext): Future[ServiceTask[BuildRequest, ModelVersion]] = {
    modelBuildRepository.get(taskId).map(_.get.toBuildTask)
  }

  override protected def updateTaskStorage(task: ServiceTask[BuildRequest, ModelVersion])(implicit ec: ExecutionContext) = {
    modelBuildRepository.update(ModelBuild.fromBuildTask(task))
  }
}

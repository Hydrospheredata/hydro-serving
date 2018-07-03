package io.hydrosphere.serving.manager.service.model_build

import io.hydrosphere.serving.manager.model.db.{BuildRequest, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.util.task.{ServiceTask, ServiceTaskExecutor, ServiceTaskUpdater}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

case class ModelBuildUpdater(
  taskId: Long,
  executor: ServiceTaskExecutor[BuildRequest, ModelVersion],
  modelBuildRepository: ModelBuildRepository
) extends ServiceTaskUpdater[BuildRequest, ModelVersion] with Logging {

  override def log(log: String): Unit = logger.info(s"Model build $taskId: " + log)

  override def task()(implicit ec: ExecutionContext): Future[ServiceTask[BuildRequest, ModelVersion]] = {
    modelBuildRepository.get(taskId).map(_.get.toBuildTask)
  }

  override protected def updateTaskStorage(task: ServiceTask[BuildRequest, ModelVersion])(implicit ec: ExecutionContext) = {
    modelBuildRepository.update(ModelBuild.fromBuildTask(task)).map(_ => Unit)
  }
}

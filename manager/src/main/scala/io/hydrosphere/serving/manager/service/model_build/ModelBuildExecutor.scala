package io.hydrosphere.serving.manager.service.model_build

import java.time.LocalDateTime

import com.spotify.docker.client.ProgressHandler
import com.spotify.docker.client.messages.ProgressMessage
import io.hydrosphere.serving.manager.model.db.{BuildRequest, ModelBuild, ModelVersion}
import io.hydrosphere.serving.model.api.{HFResult, HResult, Result}
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.service.model_build.builders.ModelBuildService
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.util.task.{ExecFuture, ServiceTask, ServiceTaskExecutor, ServiceTaskUpdater}

import scala.concurrent.{ExecutionContext, Future}

class ModelBuildExecutor(
  val executionContext: ExecutionContext,
  val modelBuildRepository: ModelBuildRepository,
  val modelVersionSerivce: ModelVersionManagementService,
  val modelBuildService: ModelBuildService
) extends ServiceTaskExecutor[BuildRequest, ModelVersion] {
  implicit val ec = executionContext

  override def makeUpdater(request: BuildRequest)
  : Future[ServiceTaskUpdater[BuildRequest, ModelVersion]] = {

    val modelBuildTask = ServiceTask.create[BuildRequest, ModelVersion](0, LocalDateTime.now(), request)
    modelBuildRepository.create(ModelBuild.fromBuildTask(modelBuildTask)).map { actualModelBuild =>
      ModelBuildUpdater(actualModelBuild.id, this, modelBuildRepository)
    }
  }

  override def execute(request: BuildRequest): ExecFuture[BuildRequest, ModelVersion] = {
    runRequestF(request)(handleBuild)
  }

  def handleBuild(
    buildRequest: BuildRequest,
    updater: ServiceTaskUpdater[BuildRequest, ModelVersion]
  ): Future[ModelVersion] = {

    logger.info(s"Building ${buildRequest.model.name} v${buildRequest.version}")
    logger.debug(buildRequest)

    val messageHandler = new ProgressHandler {
      override def progress(message: ProgressMessage): Unit = {
        val msg = Option(message.stream()).getOrElse("")
        updater.log(msg)
      }
    }

    updater.running().flatMap { task =>
      val modelBuild = ModelBuild.fromBuildTask(task)
      modelBuildService
        .build(modelBuild, buildRequest.script, messageHandler)
        .flatMap(afterBuild(updater, _))(ec)
        .flatMap {
          case Left(value) =>
            updater.failed(value.message, LocalDateTime.now())
            throw new IllegalStateException(value.message)
          case Right(value) =>
            updater.finished(value).map(_ => value)
        }
    }
  }

  def afterBuild(
    updater: ServiceTaskUpdater[BuildRequest, ModelVersion],
    res: HResult[String]
  ): HFResult[ModelVersion] = {
    updater.task().flatMap { task =>
      val modelBuild = task.request
      res match {
        case Left(err) =>
          logger.error(s"Errors while building ${modelBuild.model.name} v${modelBuild.version}:")
          logger.error(err)
          updater.failed(err.toString)
          Result.errorF(err)
        case Right(sha256) =>
          val version = ModelVersion(
            id = 0,
            imageName = modelBuild.model.name,
            imageTag = modelBuild.version.toString,
            imageSHA256 = sha256,
            modelName = modelBuild.model.name,
            modelVersion = modelBuild.version,
            modelContract = modelBuild.model.modelContract,
            created = LocalDateTime.now,
            model = Some(modelBuild.model),
            modelType = modelBuild.model.modelType
          )
          modelVersionSerivce.create(version)
      }
    }
  }
}
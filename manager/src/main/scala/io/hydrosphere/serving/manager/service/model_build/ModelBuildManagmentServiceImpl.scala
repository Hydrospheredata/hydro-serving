package io.hydrosphere.serving.manager.service.model_build

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.service.build_script.BuildScriptManagementService
import io.hydrosphere.serving.manager.service.model_build.builders.{ModelBuildService, ModelPushService, ProgressHandler, ProgressMessage}
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ModelBuildManagmentServiceImpl(
  modelBuildRepository: ModelBuildRepository,
  buildScriptManagementService: BuildScriptManagementService,
  modelVersionManagementService: ModelVersionManagementService,
  modelManagementService: ModelManagementService,
  modelPushService: ModelPushService,
  modelBuildService: ModelBuildService
)(
  implicit executionContext: ExecutionContext
) extends ModelBuildManagmentService with Logging {

  override def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]] =
    modelBuildRepository.listByModelId(id)

  override def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]] =
    modelBuildRepository.lastByModelId(id, maximum)

  def buildNewModelVersion(model: Model, modelVersion: Option[Long]): HFResult[ModelVersion] = {
    modelVersionManagementService.fetchLastModelVersion(model.id, modelVersion).flatMap {
      case Right(version) =>
        val build = ModelBuild(
          id = 0,
          model = model,
          version = version,
          started = LocalDateTime.now(),
          finished = None,
          status = ModelBuildStatus.STARTED,
          statusText = None,
          logsUrl = None,
          modelVersion = None
        )
        buildScriptManagementService.fetchScriptForModel(model).flatMap { script =>
          modelBuildRepository.create(build).flatMap { modelBuild =>
            buildModelRuntime(modelBuild, script).transform(
              _.right.map { runtime =>
                modelBuildRepository.finishBuild(modelBuild.id, ModelBuildStatus.FINISHED, "OK", LocalDateTime.now(), Some(runtime))
                runtime
              },
              { ex =>
                logger.error(ex.getMessage, ex)
                modelBuildRepository.finishBuild(modelBuild.id, ModelBuildStatus.ERROR, ex.getMessage, LocalDateTime.now(), None)
                ex
              })
          }
        }
      case Left(err) => Result.errorF(err)
    }
  }

  def buildModelRuntime(modelBuild: ModelBuild, script: String): HFResult[ModelVersion] = {
    val handler = new ProgressHandler {
      override def handle(progressMessage: ProgressMessage): Unit =
        logger.info(progressMessage)
    }

    val imageName = modelPushService.getImageName(modelBuild)
    modelBuildService.build(modelBuild, imageName, script, handler).flatMap {
      case Left(err) => Result.errorF(err)
      case Right(sha256) =>
        val version = ModelVersion(
          id = 0,
          imageName = imageName,
          imageTag = modelBuild.version.toString,
          imageSHA256 = sha256,
          modelName = modelBuild.model.name,
          modelVersion = modelBuild.version,
          source = Some(modelBuild.model.source),
          modelContract = modelBuild.model.modelContract,
          created = LocalDateTime.now,
          model = Some(modelBuild.model),
          modelType = modelBuild.model.modelType
        )
        modelVersionManagementService.create(version).map { result =>
          result.right.map { modelVersion =>
            modelPushService.push(modelVersion, handler)
            modelVersion
          }
        }
    }
  }

  override def buildModel(modelId: Long, flatContract: Option[ContractDescription], modelVersion: Option[Long]): HFResult[ModelVersion] = {
    modelManagementService.getModel(modelId).flatMap {
      case Left(err) => Result.errorF(err)
      case Right(model) =>
        val newModel = flatContract match {
          case Some(newContract) =>
            modelManagementService.submitFlatContract(model.id, newContract)
          case None => Result.okF(model)
        }
        newModel.flatMap {
          case Left(err) => Result.errorF(err)
          case Right(rModel) => buildNewModelVersion(rModel, modelVersion)
        }
    }
  }

  override def lastForModels(ids: Seq[Long]): Future[Seq[ModelBuild]] = {
    modelBuildRepository.lastForModels(ids)
  }
}

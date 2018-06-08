package io.hydrosphere.serving.manager.service.model_build

import java.time.LocalDateTime

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.model.Result.HError
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.service.build_script.BuildScriptManagementService
import io.hydrosphere.serving.manager.service.model_build.builders._
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ModelBuildManagementServiceImpl(
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

  def build(model: Model, modelVersion: Option[Long]): HFResult[ModelBuild] = {
    logger.debug(model)
    logger.debug(modelVersion)
    val f = for {
      version <- EitherT(modelVersionManagementService.fetchLastModelVersion(model.id, modelVersion))
      script <- EitherT.liftF(buildScriptManagementService.fetchScriptForModel(model))
      build = ModelBuild(
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
      uniqueBuild <- EitherT(ensureUniqueBuild(build))
      modelBuild <- EitherT.liftF[Future, HError, ModelBuild](modelBuildRepository.create(uniqueBuild))
    } yield {
      Future(handleBuild(modelBuild, script))
      logger.debug(build)
      modelBuild
    }
    f.value
  }

  def handleBuild(modelBuild: ModelBuild, script: String): HFResult[ModelVersion] = {
    logger.info(s"Building ${modelBuild.model.name} v${modelBuild.version}")
    logger.debug(modelBuild)
    val imageName = modelPushService.getImageName(modelBuild)
    val messageHandler = new HistoricProgressHandler()
    modelBuildService.build(modelBuild, imageName, script, messageHandler).flatMap {
      case Left(err) =>
        logger.error(s"Errors while building ${modelBuild.model.name} v${modelBuild.version}:")
        logger.error(err)
        modelBuildRepository.finishBuild(modelBuild.id, ModelBuildStatus.ERROR, err.toString, LocalDateTime.now(), None)
        Result.errorF(err)
      case Right(sha256) =>
        val version = ModelVersion(
          id = 0,
          imageName = imageName,
          imageTag = modelBuild.version.toString,
          imageSHA256 = sha256,
          modelName = modelBuild.model.name,
          modelVersion = modelBuild.version,
          modelContract = modelBuild.model.modelContract,
          created = LocalDateTime.now,
          model = Some(modelBuild.model),
          modelType = modelBuild.model.modelType
        )
        modelVersionManagementService.create(version).map { result =>
          result.right.map { modelVersion =>
            modelPushService.push(modelVersion, InfoProgressHandler)
            modelBuildRepository.finishBuild(modelBuild.id, ModelBuildStatus.FINISHED, "OK", LocalDateTime.now(), Some(modelVersion))
            logger.info(s"${modelBuild.model.name} v${modelBuild.version} built successfully")
            modelVersion
          }
        }
    }
  }

  override def buildAndOverrideContract(modelId: Long, flatContract: Option[ContractDescription], modelVersion: Option[Long]): HFResult[ModelBuild] = {
    logger.debug(s"modelId=$modelId,flatContract=$flatContract modelVersion=$modelVersion")
    val f = for {
      model <- EitherT(modelManagementService.getModel(modelId))
      newModel <- EitherT(maybeUpdateContract(model, flatContract))
      version <- EitherT(build(newModel, modelVersion))
    } yield version
    f.value
  }

  private def maybeUpdateContract(model: Model, flatContract: Option[ContractDescription]) = {
    flatContract match {
      case Some(newContract) =>
        logger.debug("New contract applied")
        modelManagementService.submitFlatContract(model.id, newContract)
      case None =>
        logger.debug("Old contract intact")
        Result.okF(model)
    }
  }

  override def lastForModels(ids: Seq[Long]): Future[Seq[ModelBuild]] = {
    modelBuildRepository.lastForModels(ids)
  }

  def uploadAndBuild(modelUpload: ModelUpload): HFResult[ModelBuild] = {
    val f = for {
      model <- EitherT(modelManagementService.uploadModel(modelUpload))
      version <- EitherT(buildAndOverrideContract(model.id))
    } yield version
    f.value
  }

  /**
    * Ensures there is no unfinished build for given modelId and version
    *
    * @param modelBuild build to check
    * @return Right if there is no duplicating build. Left otherwise
    */
  def ensureUniqueBuild(modelBuild: ModelBuild): HFResult[ModelBuild] = {
    modelBuildRepository.getRunningBuild(modelBuild.model.id, modelBuild.version).map {
      case Some(x) => Result.clientError(s"There is already a running build for a model ${x.model.name} version ${x.version}")
      case None => Result.ok(modelBuild)
    }
  }
}

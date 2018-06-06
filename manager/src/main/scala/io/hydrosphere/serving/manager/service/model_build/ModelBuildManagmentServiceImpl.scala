package io.hydrosphere.serving.manager.service.model_build

import java.nio.file.Path
import java.time.LocalDateTime

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.manager.controller.application.{ExecutionGraphRequest, ExecutionStepRequest, SimpleServiceDescription}
import io.hydrosphere.serving.manager.controller.model.{ModelDeploy, ModelUpload}
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{Application, Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.build_script.BuildScriptManagementService
import io.hydrosphere.serving.manager.service.model_build.builders._
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.service.runtime.RuntimeManagementService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ModelBuildManagmentServiceImpl(
  modelBuildRepository: ModelBuildRepository,
  buildScriptManagementService: BuildScriptManagementService,
  modelVersionManagementService: ModelVersionManagementService,
  modelManagementService: ModelManagementService,
  modelPushService: ModelPushService,
  modelBuildService: ModelBuildService,
  runtimeManagementService: RuntimeManagementService,
  applicationManagementService: ApplicationManagementService
)(
  implicit executionContext: ExecutionContext
) extends ModelBuildManagmentService with Logging {

  override def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]] =
    modelBuildRepository.listByModelId(id)

  override def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]] =
    modelBuildRepository.lastByModelId(id, maximum)

  def buildNewModelVersion(model: Model, modelVersion: Option[Long]): HFResult[ModelVersion] = {
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
      modelBuild <- EitherT.liftF(modelBuildRepository.create(uniqueBuild))
      modelVersion <- EitherT(buildModelVersion(modelBuild, script))
    } yield {
      modelVersion
    }
    f.value
  }

  def buildModelVersion(modelBuild: ModelBuild, script: String): HFResult[ModelVersion] = {
    val imageName = modelPushService.getImageName(modelBuild)
    modelBuildService.build(modelBuild, imageName, script, InfoProgressHandler).flatMap {
      case Left(err) =>
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
            modelVersion
          }
        }
    }
  }

  override def buildModel(modelId: Long, flatContract: Option[ContractDescription], modelVersion: Option[Long]): HFResult[ModelVersion] = {
    val f = for {
      model <- EitherT(modelManagementService.getModel(modelId))
      newModel <- EitherT(maybeUpdateContract(model, flatContract))
      version <- EitherT(buildNewModelVersion(newModel, modelVersion))
    } yield version
    f.value
  }

  private def maybeUpdateContract(model: Model, flatContract: Option[ContractDescription]) = {
    flatContract match {
      case Some(newContract) =>
        modelManagementService.submitFlatContract(model.id, newContract)
      case None => Result.okF(model)
    }
  }

  override def lastForModels(ids: Seq[Long]): Future[Seq[ModelBuild]] = {
    modelBuildRepository.lastForModels(ids)
  }

  def uploadAndBuild(modelTarball: Path, modelUpload: ModelUpload): HFResult[ModelVersion] = {
    val f = for {
      model <- EitherT(modelManagementService.uploadModel(modelTarball, modelUpload))
      version <- EitherT(buildModel(model.id))
    } yield version
    f.value
  }

  /**
    * Ensures there is no build unfinished build for given modelId and version
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

  /**
    * Multipurpose method that uploades, releases a model, and creates a simple app with it
    * @param deploy deployment definition
    * @return created application
    */
  override def uploadAndDeploy(file: Path, deploy: ModelDeploy): HFResult[Application] = {
    val f = for {
      version <- EitherT(uploadAndBuild(file, deploy.model))
      runtime <- EitherT(runtimeManagementService.get(deploy.runtimeName, deploy.runtimeVersion))
      executionGraphRequest = ExecutionGraphRequest(
        Seq(ExecutionStepRequest(
            List(SimpleServiceDescription(
              runtimeId = runtime.id,
              modelVersionId = Some(version.id),
              None,
              100,
              "default"
            ))
          ))
      )
      app <- EitherT(applicationManagementService.createApplication(
        version.fullName,
        version.model.flatMap(_.namespace),
        executionGraphRequest,
        Seq.empty
      ))
    } yield app
    f.value
  }
}

package io.hydrosphere.serving.manager.service.model_build

import java.util.concurrent.Executors

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.model.api.Result.Implicits._
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.model.api.description.ContractDescription
import io.hydrosphere.serving.manager.model.db.{BuildRequest, Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.service.build_script.BuildScriptManagementService
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_build.builders._
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.util.task.ExecFuture
import io.hydrosphere.serving.model.api.{HFResult, Result}
import io.hydrosphere.serving.model.api.Result.{ClientError, HError}
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
  val buildTaskExecutor = new ModelBuildExecutor(
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4)),
    modelBuildRepository,
    modelVersionManagementService,
    modelBuildService
  )

  override def get(buildId: Long): HFResult[ModelBuild] = {
    modelBuildRepository.get(buildId).map { maybeBuild =>
      maybeBuild.toHResult(ClientError(s"Can't find build for id=$buildId"))
    }
  }

  override def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]] =
    modelBuildRepository.listByModelId(id)

  override def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]] =
    modelBuildRepository.lastByModelId(id, maximum)

  def build(model: Model, modelVersion: Option[Long]): HFResult[ExecFuture[BuildRequest, ModelVersion]] = {
    logger.debug(model)
    logger.debug(modelVersion)
    val f = for {
      version <- EitherT(modelVersionManagementService.fetchLastModelVersion(model.id, modelVersion))
      script <- EitherT.liftF(buildScriptManagementService.fetchScriptForModel(model))
      build = BuildRequest(
        model = model,
        version = version,
        script = script
      )
      uniqueBuild <- EitherT(ensureUniqueBuild(build))
    } yield {
      logger.debug(build)
      buildTaskExecutor.execute(uniqueBuild)
    }
    f.value
  }

  override def buildModel(buildModelRequest: BuildModelRequest): HFResult[ExecFuture[BuildRequest, ModelVersion]] = {
    logger.debug(
      s"modelId=${buildModelRequest.modelId}," +
        s"flatContract=${buildModelRequest.flatContract}" +
        s" modelVersion=${buildModelRequest.modelVersion}"
    )
    val f = for {
      model <- EitherT(modelManagementService.getModel(buildModelRequest.modelId))
      newModel <- EitherT(maybeUpdateContract(model, buildModelRequest.flatContract))
      version <- EitherT(build(newModel, buildModelRequest.modelVersion))
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

  def uploadAndBuild(modelUpload: ModelUpload): HFResult[ExecFuture[BuildRequest, ModelVersion]] = {
    val f = for {
      model <- EitherT(modelManagementService.uploadModel(modelUpload))
      version <- EitherT(buildModel(BuildModelRequest(model.id)))
    } yield version
    f.value
  }

  /**
    * Ensures there is no unfinished build for given modelId and version
    *
    * @param modelBuild build to check
    * @return Right if there is no duplicating build. Left otherwise
    */
  def ensureUniqueBuild(modelBuild: BuildRequest): HFResult[BuildRequest] = {
    modelBuildRepository.getRunningBuild(modelBuild.model.id, modelBuild.version).map {
      case Some(x) => Result.clientError(s"There is already a running build for a model ${x.model.name} version ${x.version}")
      case None => Result.ok(modelBuild)
    }
  }

  override def delete(buildId: Long): HFResult[ModelBuild] = {
    val f = for {
      build <- EitherT(get(buildId))
      _ <- EitherT.liftF[Future, HError, Int](modelBuildRepository.delete(buildId))
    } yield build
    f.value
  }

  override def listForModel(modelId: Long): HFResult[Seq[ModelBuild]] = {
    val f = for {
      model <- EitherT(modelManagementService.getModel(modelId))
      builds <- EitherT.liftF[Future, HError, Seq[ModelBuild]](modelBuildRepository.listByModelId(model.id))
    } yield builds
    f.value
  }
}
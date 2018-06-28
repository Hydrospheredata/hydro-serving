package io.hydrosphere.serving.manager.service.model_build

import java.time.LocalDateTime
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.spotify.docker.client.ProgressHandler
import com.spotify.docker.client.messages.ProgressMessage
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model.Result.{ClientError, HError}
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.description.ContractDescription
import io.hydrosphere.serving.manager.model.db.{Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.service.build_script.BuildScriptManagementService
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_build.builders._
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.service.{ServiceTask, ServiceTaskExecutor, ServiceTaskUpdater}
import io.hydrosphere.serving.manager.util.docker.InfoProgressHandler
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

case class BuildModelWithScript(
  modelBuild: ModelBuild,
  script: String
)

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
  val buildTaskExecutor = ServiceTaskExecutor.withFixedPool[BuildModelWithScript, ModelVersion](4)

  def get(buildId: Long): HFResult[ModelBuild] = {
    modelBuildRepository.get(buildId).map { maybeBuild =>
      maybeBuild.toHResult(ClientError(s"Can't find build for id=$buildId"))
    }
  }

  override def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]] =
    modelBuildRepository.listByModelId(id)

  override def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]] =
    modelBuildRepository.lastByModelId(id, maximum)

  def build(model: Model, modelVersion: Option[Long]): HFResult[ServiceTask[BuildModelWithScript, ModelVersion]] = {
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
      val b = BuildModelWithScript(modelBuild, script)
      logger.debug(build)
      val f = buildTaskExecutor.runRequestF(b)(handleBuild)
      f.taskStatus
    }
    f.value
  }

  def handleBuild(
    buildWithScript: BuildModelWithScript,
    updater: ServiceTaskUpdater[BuildModelWithScript, ModelVersion],
    ec: ExecutionContext): HFResult[ModelVersion] = {

    updater.running()
    val modelBuild = buildWithScript.modelBuild
    val script = buildWithScript.script
    logger.info(s"Building ${modelBuild.model.name} v${modelBuild.version}")
    logger.debug(modelBuild)
    val imageName = modelPushService.getImageName(modelBuild)
    val messageHandler = new ProgressHandler {
      override def progress(message: ProgressMessage): Unit = {
        val msg = Option(message.stream()).getOrElse("")
        updater.log(msg)
      }
    }

    modelBuildService.build(modelBuild, imageName, script, messageHandler)
      .flatMap(afterBuild(modelBuild, imageName, _))(ec).map { res =>
      res match {
        case Left(value) => updater.failed(value.message, LocalDateTime.now())
        case Right(value) => updater.finished(value)
      }
      res
    }

  }

  def afterBuild(modelBuild: ModelBuild, imageName: String, res: HResult[String]): HFResult[ModelVersion] = {
    res match {
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

  override def buildModel(buildModelRequest: BuildModelRequest): HFResult[ServiceTask[BuildModelWithScript, ModelVersion]] = {
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

  def uploadAndBuild(modelUpload: ModelUpload): HFResult[ServiceTask[BuildModelWithScript, ModelVersion]] = {
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
  def ensureUniqueBuild(modelBuild: ModelBuild): HFResult[ModelBuild] = {
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

  override def getBuildStatus(id: UUID): HResult[ServiceTask[BuildModelWithScript, ModelVersion]] = {
    val res = buildTaskExecutor.taskInfos.get(id).toHResult(ClientError(s"Can't find request ${id.toString}"))
    logger.debug(s"Status for runtime creation request $id = $res")
    res
  }
}
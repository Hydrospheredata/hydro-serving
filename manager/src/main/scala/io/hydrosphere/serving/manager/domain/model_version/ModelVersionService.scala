package io.hydrosphere.serving.manager.domain.model_version

import java.time.LocalDateTime

import cats.data.EitherT
import cats.implicits._
import com.spotify.docker.client.ProgressHandler
import com.spotify.docker.client.messages.ProgressMessage
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.application.ApplicationRepositoryAlgebra
import io.hydrosphere.serving.manager.domain.build_script.BuildScriptService
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorService
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.infrastructure.storage.StorageUploadResult
import io.hydrosphere.serving.model.api.Result.HError
import io.hydrosphere.serving.model.api.{HFResult, ModelType, Result}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ModelVersionService(
  modelVersionRepository: ModelVersionRepositoryAlgebra[Future],
  buildScriptService: BuildScriptService,
  hostSelectorService: HostSelectorService,
  modelBuildService: ModelBuildAlgebra,
  modelPushService: ModelVersionPushAlgebra,
  applicationRepo: ApplicationRepositoryAlgebra[Future]
)(
  implicit executionContext: ExecutionContext
) extends Logging {

  def deleteVersions(mvs: Seq[ModelVersion]): HFResult[Seq[ModelVersion]] = {
    Result.traverseF(mvs) { version =>
      delete(version.id)
    }
  }

  def fetchLastModelVersion(modelId: Long, modelVersion: Option[Long]): HFResult[Long] = {
    modelVersion match {
      case Some(x) => modelVersionRepository.modelVersionByModelAndVersion(modelId, x).map {
        case None =>
          logger.debug(s"modelId=$modelId modelVersion=$modelVersion modelVersion=$x")
          Right(x)
        case _ =>
          logger.error(s"$modelVersion already exists")
          Result.clientError(s"$modelVersion already exists")
      }
      case None => modelVersionRepository.lastModelVersionByModel(modelId, 1).map { se =>
        val result = nextVersion(se.headOption)
        logger.debug(s"modelId=$modelId modelVersion=$modelVersion lastModelVersionByModel=${se.map(_.modelVersion)} nextVersion=$result")
        Right(result)
      }
    }
  }

  def lastModelVersionByModelId(id: Long, maximum: Int): Future[Seq[ModelVersion]] = {
    modelVersionRepository.lastModelVersionByModel(id: Long, maximum: Int)
  }

  def get(key: Long): HFResult[ModelVersion] = {
    modelVersionRepository.get(key).map {
      case Some(model) => Right(model)
      case None => Result.clientError(s"Can't find a model version with id: $key")
    }
  }

  def get(key: Set[Long]): Future[Seq[ModelVersion]] = {
    modelVersionRepository.get(key.toSeq)
  }

  def list: Future[Seq[ModelVersionView]] = {
    for {
      allVersions <- modelVersionRepository.all()
      f <- Future.traverse(allVersions.map(_.id)) { x =>
        applicationRepo.findVersionsUsage(x).map(x -> _)
      }
      usageMap = f.toMap
    } yield {
      allVersions.map { v =>
        ModelVersionView.fromVersion(v, usageMap.getOrElse(v.id, Seq.empty))
      }
    }
  }

  private def nextVersion(lastModel: Option[ModelVersion]): Long = lastModel match {
    case None => 1
    case Some(modelVersion) => modelVersion.modelVersion + 1
  }

  def create(version: ModelVersion): HFResult[ModelVersion] = {
    modelVersionRepository.create(version).map(Result.ok)
  }

  def modelVersionsByModelVersionIds(modelIds: Set[Long]): Future[Seq[ModelVersion]] = {
    modelVersionRepository.modelVersionsByModelVersionIds(modelIds)
  }

  def listForModel(modelId: Long): HFResult[Seq[ModelVersion]] = {
    modelVersionRepository.listForModel(modelId).map(Result.ok)
  }

  def delete(versionId: Long): HFResult[ModelVersion] = {
    val f = for {
      version <- EitherT(get(versionId))
      _ <- EitherT.liftF[Future, HError, Int](modelVersionRepository.delete(versionId))
    } yield {
      version
    }
    f.value
  }

  def build(model: Model, modelUpload: ModelUploadMetadata, storageUploadResult: StorageUploadResult): HFResult[ModelVersion] = {
    logger.debug(model)
    val messageHandler = new ProgressHandler {
      override def progress(message: ProgressMessage): Unit = {
        val msg = Option(message.stream()).getOrElse("")
        logger.info(msg)
      }
    }
    val f = for {
      version <- EitherT(fetchLastModelVersion(model.id, None))
      modelType = modelUpload.modelType.map(ModelType.fromTag).getOrElse(storageUploadResult.modelType)
      script <- EitherT.liftF(buildScriptService.fetchScriptForModelType(modelType))
      contract = modelUpload.contract.getOrElse(storageUploadResult.modelContract)
      image = modelPushService.getImage(model.name, version)
      mv = ModelVersion(
        id = 0,
        image = image,
        created = LocalDateTime.now(),
        finished = None,
        modelVersion = version,
        modelType = modelType,
        modelContract = contract,
        runtime = DockerImage(
          name = modelUpload.runtimeName,
          tag = modelUpload.runtimeVersion
        ),
        model = model,
        hostSelector = None, // TODO Fix later
        status = ModelVersionStatus.Started
      )
      modelVersion <- EitherT(create(mv))
    } yield (script, modelVersion)

    f.map {
      case (script, mv) =>
        val res = EitherT(modelBuildService.build(mv.model.name, mv.modelVersion, mv.modelType, mv.modelContract, mv.image, script, messageHandler))
          .map { sha =>
            val newDockerImage = mv.image.copy(sha256 = Some(sha))
            mv.copy(image = newDockerImage, finished = Some(LocalDateTime.now()))
          }.value

        res.map {
          case Left(err) =>
            logger.error(err)
            val failed = mv.copy(status = ModelVersionStatus.Failed)
            modelVersionRepository.update(failed.id, failed)
          case Right(smv) =>
            val finished = smv.copy(status = ModelVersionStatus.Finished)
            modelVersionRepository.update(finished.id, finished)
        }.failed.foreach { err =>
          logger.error(err)
          val failed = mv.copy(status = ModelVersionStatus.Failed)
          modelVersionRepository.update(failed.id, failed)
        }
    }

    f.map(_._2).value
  }
}
package io.hydrosphere.serving.manager.domain.model_version

import java.time.LocalDateTime

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.domain.application.ApplicationRepositoryAlgebra
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorServiceAlg
import io.hydrosphere.serving.manager.domain.image.{ImageBuilder, ImageRepository}
import io.hydrosphere.serving.manager.domain.model.{Model, ModelVersionMetadata}
import io.hydrosphere.serving.manager.domain.model_build.ModelFilePacker
import io.hydrosphere.serving.model.api.Result.HError
import io.hydrosphere.serving.model.api.{HFResult, Result}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}


trait ModelVersionServiceAlg {
  def deleteVersions(mvs: Seq[ModelVersion]): HFResult[Seq[ModelVersion]]

  def getNextModelVersion(modelId: Long): HFResult[Long]

  def list: Future[Seq[ModelVersionView]]

  def modelVersionsByModelVersionIds(modelIds: Set[Long]): Future[Seq[ModelVersion]]

  def listForModel(modelId: Long): HFResult[Seq[ModelVersion]]

  def delete(versionId: Long): HFResult[ModelVersion]

  def build(model: Model, metadata: ModelVersionMetadata): HFResult[BuildResult]
}

class ModelVersionService(
  modelVersionRepository: ModelVersionRepositoryAlgebra[Future],
  hostSelectorService: HostSelectorServiceAlg,
  modelFilePacker: ModelFilePacker[Future],
  imageBuilder: ImageBuilder[Future],
  imageRepository: ImageRepository[Future],
  applicationRepo: ApplicationRepositoryAlgebra[Future]
)(implicit executionContext: ExecutionContext) extends ModelVersionServiceAlg with Logging {

  def deleteVersions(mvs: Seq[ModelVersion]): HFResult[Seq[ModelVersion]] = {
    Result.traverseF(mvs) { version =>
      delete(version.id)
    }
  }

  def getNextModelVersion(modelId: Long): HFResult[Long] = {
    modelVersionRepository.lastModelVersionByModel(modelId, 1).map { se =>
      val result = se.headOption.map(_.modelVersion + 1).getOrElse(1L)
      Right(result)
    }
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

  def modelVersionsByModelVersionIds(modelIds: Set[Long]): Future[Seq[ModelVersion]] = {
    modelVersionRepository.modelVersionsByModelVersionIds(modelIds)
  }

  def listForModel(modelId: Long): HFResult[Seq[ModelVersion]] = {
    modelVersionRepository.listForModel(modelId).map(Result.ok)
  }

  def delete(versionId: Long): HFResult[ModelVersion] = {
    val f = for {
      version <- EitherT.fromOptionF(modelVersionRepository.get(versionId), Result.ClientError(s"Can't find the version with id $versionId"))
      _ <- EitherT.liftF[Future, HError, Int](modelVersionRepository.delete(versionId))
    } yield version
    f.value
  }

  def build(model: Model, metadata: ModelVersionMetadata): HFResult[BuildResult] = {
    logger.debug(model)
    val f = for {
      version <- EitherT(getNextModelVersion(model.id))
      image = imageRepository.getImage(metadata.modelName, version.toString)
      mv = ModelVersion(
        id = 0,
        image = image,
        created = LocalDateTime.now(),
        finished = None,
        modelVersion = version,
        modelContract = metadata.contract,
        runtime = metadata.runtime,
        model = model,
        hostSelector = metadata.hostSelector,
        status = ModelVersionStatus.Assembling,
        profileTypes = metadata.profileTypes
      )
      modelVersion <- EitherT.liftF[Future, HError, ModelVersion](modelVersionRepository.create(mv))
    } yield modelVersion

    val completedBuild = f.value.flatMap {
      case Left(err) =>
        logger.error(err)
        Future.failed(new IllegalArgumentException(err.toString))
      case Right(mv) =>
        val f = for {
          buildPath <- modelFilePacker.pack(BuildRequest.fromVersion(mv))
          imageSha <- imageBuilder.build(buildPath, mv.image)
          newDockerImage = mv.image.copy(sha256 = Some(imageSha))
          finishedVersion = mv.copy(image = newDockerImage, finished = Some(LocalDateTime.now()), status = ModelVersionStatus.Finished)
          _ <- modelVersionRepository.update(finishedVersion.id, finishedVersion)
          _ <- imageRepository.push(finishedVersion.image)
        } yield finishedVersion

        f.failed.foreach { err =>
          logger.error(err)
          val failed = mv.copy(status = ModelVersionStatus.Failed)
          modelVersionRepository.update(failed.id, failed)
        }
        f
    }

    f.map(t => BuildResult(t, completedBuild)).value
  }
}
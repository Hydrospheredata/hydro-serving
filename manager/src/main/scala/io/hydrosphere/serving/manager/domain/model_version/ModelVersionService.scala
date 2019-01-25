package io.hydrosphere.serving.manager.domain.model_version

import java.time.LocalDateTime

import cats.Traverse
import cats.data.OptionT
import cats.effect.Effect
import cats.implicits._
import io.hydrosphere.serving.manager.domain.application.ApplicationRepository
import io.hydrosphere.serving.manager.domain.image.{ImageBuilder, ImageRepository}
import io.hydrosphere.serving.manager.domain.model.{Model, ModelVersionMetadata}
import io.hydrosphere.serving.manager.domain.model_build.ModelFilePacker
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext


trait ModelVersionService[F[_]] {
  def deleteVersions(mvs: Seq[ModelVersion]): F[Seq[ModelVersion]]

  def getNextModelVersion(modelId: Long): F[Long]

  def list: F[Seq[ModelVersionView]]

  def modelVersionsByModelVersionIds(modelIds: Set[Long]): F[Seq[ModelVersion]]

  def delete(versionId: Long): F[Option[ModelVersion]]

  def build(model: Model, metadata: ModelVersionMetadata): F[BuildResult]
}

object ModelVersionService {
  def apply[F[_] : Effect](
    modelVersionRepository: ModelVersionRepository[F],
    modelFilePacker: ModelFilePacker[F],
    imageBuilder: ImageBuilder[F],
    imageRepository: ImageRepository[F],
    applicationRepo: ApplicationRepository[F]
  )(implicit executionContext: ExecutionContext): ModelVersionService[F] = new ModelVersionService[F] with Logging {

    def deleteVersions(mvs: Seq[ModelVersion]): F[Seq[ModelVersion]] = {
      Traverse[List].traverse(mvs.toList) { version =>
        delete(version.id)
      }.map(_.flatten)
    }

    def getNextModelVersion(modelId: Long): F[Long] = {
      for {
        versions <- modelVersionRepository.lastModelVersionByModel(modelId, 1)
      } yield versions.headOption.fold(1L)(_.modelVersion + 1)
    }

    def list: F[Seq[ModelVersionView]] = {
      for {
        allVersions <- modelVersionRepository.all()
        f <- Traverse[List].traverse(allVersions.map(_.id).toList) { x =>
          applicationRepo.findVersionsUsage(x).map(x -> _)
        }
        usageMap = f.toMap
      } yield {
        allVersions.map { v =>
          ModelVersionView.fromVersion(v, usageMap.getOrElse(v.id, Seq.empty))
        }
      }
    }

    def modelVersionsByModelVersionIds(modelIds: Set[Long]): F[Seq[ModelVersion]] = {
      modelVersionRepository.modelVersionsByModelVersionIds(modelIds)
    }

    def delete(versionId: Long): F[Option[ModelVersion]] = {
      val f = for {
        version <- OptionT(modelVersionRepository.get(versionId))
        _ <- OptionT.liftF(modelVersionRepository.delete(versionId))
      } yield version
      f.value
    }

    def build(model: Model, metadata: ModelVersionMetadata): F[BuildResult] = {
      val f = for {
        version <- getNextModelVersion(model.id)
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
        modelVersion <- modelVersionRepository.create(mv)
      } yield modelVersion

      val completed = f.flatMap { mv =>
        val innerCompleted = for {
          buildPath <- modelFilePacker.pack(BuildRequest.fromVersion(mv))
          imageSha <- imageBuilder.build(buildPath, mv.image)
          newDockerImage = mv.image.copy(sha256 = Some(imageSha))
          finishedVersion = mv.copy(image = newDockerImage, finished = Some(LocalDateTime.now()), status = ModelVersionStatus.Released)
          _ <- modelVersionRepository.update(finishedVersion.id, finishedVersion)
          _ <- imageRepository.push(finishedVersion.image)
        } yield finishedVersion

        Effect[F].onError(innerCompleted) {
          case err =>
            logger.error(err)
            val failed = mv.copy(status = ModelVersionStatus.Failed)
            modelVersionRepository.update(failed.id, failed).map(_ => ())
        }
      }

      f.map(t => BuildResult(
        t,
        Effect[F].toIO(completed).unsafeToFuture()
      ))
    }
  }
}
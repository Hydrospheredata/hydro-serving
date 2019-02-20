package io.hydrosphere.serving.manager.domain.model_build

import java.time.LocalDateTime

import cats.effect.Concurrent
import cats.effect.concurrent.Deferred
import cats.effect.implicits._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.hydrosphere.serving.manager.domain.image.{ImageBuilder, ImageRepository}
import io.hydrosphere.serving.manager.domain.model.{Model, ModelVersionMetadata}
import io.hydrosphere.serving.manager.domain.model_version._
import io.hydrosphere.serving.manager.infrastructure.storage.{ModelFileStructure, StorageOps}
import org.apache.logging.log4j.scala.Logging

trait ModelVersionBuilder[F[_]]{
  def build(model: Model, metadata: ModelVersionMetadata, modelFileStructure: ModelFileStructure): F[BuildResult[F]]
}

object ModelVersionBuilder {
  def apply[F[_] : Concurrent](
    imageBuilder: ImageBuilder[F],
    modelVersionRepository: ModelVersionRepository[F],
    imageRepository: ImageRepository[F],
    modelVersionService: ModelVersionService[F],
    storageOps: StorageOps[F]
  ): ModelVersionBuilder[F] = new ModelVersionBuilder[F] with Logging {
    override def build(model: Model, metadata: ModelVersionMetadata, modelFileStructure: ModelFileStructure): F[BuildResult[F]] = {
      for {
        init <- initialVersion(model, metadata)
        deferred <- Deferred[F, ModelVersion]
        fbr <- handleBuild(init, modelFileStructure).flatMap(deferred.complete).start
      } yield BuildResult(init, deferred)
    }

    def initialVersion(model: Model, metadata: ModelVersionMetadata) = {
      for {
        version <- modelVersionService.getNextModelVersion(model.id)
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
          profileTypes = metadata.profileTypes,
          installCommand = metadata.installCommand,
          metadata = metadata.metadata
        )
        modelVersion <- modelVersionRepository.create(mv)
      } yield modelVersion
    }

    def handleBuild(mv: ModelVersion, modelFileStructure: ModelFileStructure) = {
      val innerCompleted = for {
        buildPath <- prepare(mv, modelFileStructure)
        imageSha <- imageBuilder.build(buildPath.root, mv.image)
        newDockerImage = mv.image.copy(sha256 = Some(imageSha))
        finishedVersion = mv.copy(image = newDockerImage, finished = Some(LocalDateTime.now()), status = ModelVersionStatus.Released)
        _ <- modelVersionRepository.update(finishedVersion.id, finishedVersion)
        _ <- imageRepository.push(finishedVersion.image)
      } yield finishedVersion

      Concurrent[F].handleErrorWith(innerCompleted) { err =>
        for {
          _ <- Concurrent[F].delay(logger.error(err, err))
          failed = mv.copy(status = ModelVersionStatus.Failed)
          _ <- modelVersionRepository.update(failed.id, failed)
        } yield failed
      }
    }

    def prepare(modelVersion: ModelVersion, modelFileStructure: ModelFileStructure): F[ModelFileStructure] = {
      for {
        _ <- storageOps.writeBytes(modelFileStructure.dockerfile, BuildScript.generate(modelVersion).getBytes)
        _ <- storageOps.writeBytes(modelFileStructure.contractPath, modelVersion.modelContract.toByteArray)
      } yield modelFileStructure
    }
  }
}
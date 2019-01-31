package io.hydrosphere.serving.manager.domain.model_build

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.time.LocalDateTime

import cats.effect.Effect
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.hydrosphere.serving.manager.domain.image.{ImageBuilder, ImageRepository}
import io.hydrosphere.serving.manager.domain.model.{Model, ModelVersionMetadata}
import io.hydrosphere.serving.manager.domain.model_version._
import io.hydrosphere.serving.manager.infrastructure.storage.ModelFileStructure
import org.apache.logging.log4j.scala.Logging

trait ModelVersionBuilder[F[_]]{
  def build(model: Model, metadata: ModelVersionMetadata, modelFileStructure: ModelFileStructure): F[BuildResult]
}

object ModelVersionBuilder {
  def apply[F[_] : Effect](
    imageBuilder: ImageBuilder[F],
    modelVersionRepository: ModelVersionRepository[F],
    imageRepository: ImageRepository[F],
    modelVersionService: ModelVersionService[F]
  ): ModelVersionBuilder[F] = new ModelVersionBuilder[F] with Logging {
    override def build(model: Model, metadata: ModelVersionMetadata, modelFileStructure: ModelFileStructure): F[BuildResult] = {
      val f = for {
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
          installCommand = metadata.installCommand
        )
        modelVersion <- modelVersionRepository.create(mv)
      } yield modelVersion

      val completed = f.flatMap { mv =>
        val innerCompleted = for {
          buildPath <- prepare(mv, modelFileStructure)
          imageSha <- imageBuilder.build(buildPath.root, mv.image)
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

    def prepare(modelVersion: ModelVersion, modelFileStructure: ModelFileStructure): F[ModelFileStructure] = {
      for {
        // create Dockerfile
        _ <- Effect[F].delay {
          Files.copy(new ByteArrayInputStream(BuildScript.generate(modelVersion).getBytes), modelFileStructure.dockerfile)
        }
        // write contract to the file
        _ <- Effect[F].delay {
          Files.write(modelFileStructure.contractPath, modelVersion.modelContract.toByteArray)
        }
      } yield modelFileStructure
    }
  }
}
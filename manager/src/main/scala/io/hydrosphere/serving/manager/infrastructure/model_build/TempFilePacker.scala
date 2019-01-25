package io.hydrosphere.serving.manager.infrastructure.model_build

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path}

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.flatMap._
import io.hydrosphere.serving.manager.domain.model_build.{BuildScript, ModelFilePacker}
import io.hydrosphere.serving.manager.domain.model_version.BuildRequest
import io.hydrosphere.serving.manager.infrastructure.storage.{ModelStorage, StorageOps}
import org.apache.commons.io.FileUtils

class TempFilePacker[F[_]: Sync](
  modelStorage: ModelStorage[F],
  storageOps: StorageOps[F]
) extends ModelFilePacker[F] {
  override def pack(buildRequest: BuildRequest): F[Path] = {
    for {
      originalModelDir <- Sync[F].rethrow(
        modelStorage.getLocalPath(buildRequest.modelName).map{ x =>
          x.toRight(new IllegalArgumentException(s"Can't find folder for model ${buildRequest.modelName}").asInstanceOf[Throwable])
        }
      )
      buildPath <- storageOps.getTempDir(s"hs-model-${buildRequest.modelName}-${buildRequest.modelVersion}")
      // create Dockerfile
      _ <- Sync[F].delay {
        Files.copy(new ByteArrayInputStream(BuildScript.generate(buildRequest, buildPath).getBytes), buildPath.resolve("Dockerfile"))
      }
      // prepare and copy model files
      _ <- Sync[F].delay {
        Files.createDirectories(buildPath.resolve(BuildScript.Parameters.modelRootDir))
        FileUtils.copyDirectory(originalModelDir.toFile, buildPath.resolve(BuildScript.Parameters.modelFilesPath).toFile)
      }
      // write contract to the file
      _ <- Sync[F].delay {
        Files.write(buildPath.resolve(BuildScript.Parameters.contractFile), buildRequest.contract.toByteArray)
      }
    } yield buildPath
  }
}
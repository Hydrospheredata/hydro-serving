package io.hydrosphere.serving.manager.infrastructure.storage

import java.nio.file.{Files, Path}

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.hydrosphere.serving.manager.util.TarGzUtils

// TODO(bulat) support other formats?
trait ModelUnpacker[F[_]] {
  /**
    * Unpacks model files and returns path to it
    * @param filePath path to the tarball file
    * @return
    */
  def unpack(filePath: Path): F[ModelFileStructure]
}

object ModelUnpacker {
  def apply[F[_] : Sync](
    storageOps: StorageOps[F],
  ): ModelUnpacker[F] = (archivePath: Path) => {
    for {
      tempUnpackedDir <- storageOps.getTempDir(archivePath.getFileName.toString)
      model = ModelFileStructure.forRoot(tempUnpackedDir)
      _ <- Sync[F].delay {
        Files.createDirectories(model.filesPath)
      }
      _ <- Sync[F].delay {
        TarGzUtils.decompress(archivePath, model.filesPath)
      }
    } yield model
  }
}
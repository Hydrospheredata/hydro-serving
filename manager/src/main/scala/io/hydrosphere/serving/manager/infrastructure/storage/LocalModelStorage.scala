package io.hydrosphere.serving.manager.infrastructure.storage

import java.nio.file.{Files, Path}

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._

class LocalModelStorage[F[_]: Sync](
  storageOps: StorageOps[F],
) extends ModelStorage[F] {

  def unpack(archivePath: Path): F[ModelFileStructure] = {
    for {
      tempUnpackedDir <- storageOps.getTempDir(archivePath.getFileName.toString)
      model <- ModelFileStructure.forRoot(tempUnpackedDir)
      _ <- Sync[F].delay {
        Files.createDirectories(model.filesPath)
      }
      _ <- storageOps.unpackArchive(archivePath, model.filesPath)
    } yield model
  }
}
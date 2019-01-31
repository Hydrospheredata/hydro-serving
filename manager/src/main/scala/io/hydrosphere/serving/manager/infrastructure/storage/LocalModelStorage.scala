package io.hydrosphere.serving.manager.infrastructure.storage

import java.io.File
import java.nio.file.{Files, Path}

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._

class LocalModelStorage[F[_]: Sync](
  rootDir: Path,
  storageOps: StorageOps[F],
) extends ModelStorage[F] {

  def unpack(filePath: Path, modelFolder: Option[String]): F[Path] = {
    val modelName = modelFolder.getOrElse(filePath.getFileName.toString)
    for {
      modelDir <- Sync[F].delay(rootDir.resolve(modelName))
      _ <- storageOps.exists(modelDir).flatMap {
        case true => storageOps.removeFolder(modelDir)
        case false => Sync[F].pure(Option(()))
      }
      _ <- storageOps.unpackArchive(filePath, modelDir)
    } yield modelDir
  }

  override def getLocalPath(folderPath: String): F[Option[Path]] = {
    val f = for {
      file <- OptionT(storageOps.getReadableFile(rootDir.resolve(folderPath)))
    } yield file.toPath
    f.value
  }

  override def rename(oldFolder: String, newFolder: String): F[Option[Path]] = {
    val f = for {
      oldPath <- OptionT(getLocalPath(oldFolder))
      newPath = rootDir.resolve(newFolder)
      result <- OptionT.liftF(storageOps.moveFolder(oldPath, newPath))
    } yield result
    f.value
  }

  override def writeFile(path: String, localFile: File): F[Path] = Sync[F].defer {
    val destFile = rootDir.resolve(path)
    val parentPath = destFile.getParent

    for {
      _ <- Sync[F].delay {
        if (!Files.exists(parentPath)) {
          Files.createDirectories(parentPath)
        }
      }
      res <- storageOps.copyFile(localFile.toPath, destFile)
    } yield res
  }
}
package io.hydrosphere.serving.manager.infrastructure.storage

import java.io.File
import java.nio.file._

import cats.effect.Sync
import cats.syntax.functor._
import org.apache.commons.io.{FileUtils => ApacheFS}
import io.hydrosphere.serving.manager.util.FileUtils._

class LocalStorageOps[F[_]: Sync] extends StorageOps[F] {

  override def getReadableFile(path: Path): F[File] = Sync[F].delay {
    path.toFile
  }

  override def getSubDirs(path: Path): F[Seq[String]] = {
    getReadableFile(path).map { dirPath =>
      dirPath
        .getSubDirectories
        .map(_.getName)
        .toList
    }
  }

  override def getAllFiles(path: Path): F[Seq[String]] = {
    getReadableFile(path).map { file =>
      val fullUri = file.toURI
      file.listFilesRecursively
        .map(p => fullUri.relativize(p.toURI).toString)
        .toList
    }
  }

  override def exists(path: Path): F[Boolean] = {
    getReadableFile(path).map(_.exists())
  }

  override def copyFile(src: Path, target: Path): F[Path] = Sync[F].delay {
    Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING)
  }

  override def removeFolder(path: Path): F[Unit] = {
    getReadableFile(path).map(ApacheFS.deleteDirectory)
  }
}
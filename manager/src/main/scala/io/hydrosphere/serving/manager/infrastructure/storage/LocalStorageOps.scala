package io.hydrosphere.serving.manager.infrastructure.storage

import java.io.File
import java.nio.file._

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.functor._
import org.apache.commons.io.{FileUtils => ApacheFS}
import io.hydrosphere.serving.manager.util.FileUtils._

import scala.collection.JavaConverters._

class LocalStorageOps[F[_]: Sync] extends StorageOps[F] {

  override def getReadableFile(path: Path): F[Option[File]] = Sync[F].delay {
    if (Files.exists(path)) {
      Some(path.toFile)
    } else {
      None
    }
  }

  override def getSubDirs(path: Path): F[Option[List[String]]] = {
    OptionT(getReadableFile(path)).map { dirPath =>
      dirPath
        .getSubDirectories
        .map(_.getName)
        .toList
    }.value
  }

  override def getAllFiles(path: Path): F[Option[List[String]]] = {
    OptionT(getReadableFile(path)).map { file =>
      val fullUri = file.toURI
      file.listFilesRecursively
        .map(p => fullUri.relativize(p.toURI).toString)
        .toList
    }.value
  }

  override def exists(path: Path): F[Boolean] = {
    getReadableFile(path).map(_.isDefined)
  }

  override def copyFile(src: Path, target: Path): F[Path] = Sync[F].delay {
    val parentPath = target.getParent
    if (!Files.exists(parentPath)) {
      Files.createDirectories(parentPath)
    }
    Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING)
  }

  override def moveFolder(oldPath: Path, newPath: Path): F[Path] = Sync[F].delay {
    Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE)
  }

  override def removeFolder(path: Path): F[Option[Unit]] = {
    OptionT(getReadableFile(path)).map(ApacheFS.deleteDirectory).value
  }

  override def getTempDir(prefix: String): F[Path] = Sync[F].delay(Files.createTempDirectory(prefix))

  override def readText(path: Path): F[Option[List[String]]] = {
    OptionT(getReadableFile(path)).map(x => Files.readAllLines(x.toPath).asScala.toList).value
  }

  override def readBytes(path: Path): F[Option[Array[Byte]]] = {
    OptionT(getReadableFile(path)).map(x => Files.readAllBytes(x.toPath)).value
  }

  override def writeBytes(path: Path, bytes: Array[Byte]): F[Path] = Sync[F].delay {
    Files.write(path, bytes)
  }
}
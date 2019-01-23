package io.hydrosphere.serving.manager.infrastructure.storage

import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.config.ManagerConfiguration
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.ModelFetcher
import io.hydrosphere.serving.manager.util.TarGzUtils
import io.hydrosphere.serving.model.api.{HFResult, ModelMetadata, Result}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class LocalModelStorage[F[_]](
  rootDir: Path,
  storage: StorageOps[F]
)(implicit ex: ExecutionContext) extends ModelStorage[F] with Logging {

  def unpack(filePath: Path, folderName: Option[String]): F[ModelMetadata] = {
    val modelName = folderName.getOrElse(filePath.getFileName.toString)
    val unpackDir = Files.createTempDirectory(modelName)
    val uploadedFiles = TarGzUtils.decompress(filePath, unpackDir)
    val rootDir = Paths.get(modelName)
    storage.exists(rootDir).filter(_ == true).flatMap(_ => storage.removeFolder(rootDir))

    val localFiles = uploadedFiles
      .filter(_.startsWith(unpackDir))
      .map { path =>
        val relPath = unpackDir.relativize(path)
        path -> rootDir.resolve(relPath)
      }
      .toMap

    writeFilesToSource(localFiles)

    val inferredMeta = ModelFetcher.fetch(storage, unpackDir.toString)
    Result.okF(
      inferredMeta
    )
  }

  override def getLocalPath(folderPath: String): HFResult[Path] = {
    val f = for {
      file <- EitherT(Future.successful(storage.getReadableFile(folderPath))) // FIXME
    } yield file.toPath
    f.value
  }

  override def rename(oldFolder: String, newFolder: String): HFResult[Path] = {
    val f = for {
      oldPath <- EitherT(getLocalPath(oldFolder))
      newPath = storage.rootPath.resolve(newFolder)
      result <- EitherT(moveFolder(oldPath, newPath))
    } yield result
    f.value
  }

  override def writeFile(path: String, localFile: File): HResult[Path] = {
    val destFile = rootPath.resolve(path)
    val destPath = destFile
    val parentPath = destPath.getParent
    if (!Files.exists(parentPath)) {
      Files.createDirectories(parentPath)
    }
    Result.ok(Files.copy(localFile.toPath, destPath, StandardCopyOption.REPLACE_EXISTING))
  }

  private def moveFolder(oldPath: Path, newPath: Path) = {
    try {
      Result.okF(Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE))
    } catch {
      case ex: Exception =>
        val errMsg = s"Error while moving oldPath=$oldPath newPath=$newPath"
        logger.error(errMsg)
        Result.internalErrorF(ex, errMsg)
    }
  }
}

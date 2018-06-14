package io.hydrosphere.serving.manager.service.source

import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.ManagerConfiguration
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.service.source.fetchers.ModelFetcher
import io.hydrosphere.serving.manager.service.source.storages.ModelStorage
import io.hydrosphere.serving.manager.service.source.storages.local.LocalModelStorage
import io.hydrosphere.serving.manager.util.TarGzUtils
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ModelStorageServiceImpl(
  managerConfiguration: ManagerConfiguration)
  (implicit ex: ExecutionContext) extends ModelStorageService with Logging {

  val storage = new LocalModelStorage(managerConfiguration.localStorage)

  def upload(upload: ModelUpload): HFResult[StorageUploadResult] = { //TODO reconsider this
    try {
      val modelName = upload.name.getOrElse(upload.tarballPath.getFileName.toString)
      val unpackDir = Files.createTempDirectory(modelName)
      val rootDir = Paths.get(modelName)
      val uploadedFiles = TarGzUtils.decompress(upload.tarballPath, unpackDir)
      val localFiles = uploadedFiles
        .filter(_.startsWith(unpackDir))
        .map { path =>
          val relPath = unpackDir.relativize(path)
          path -> rootDir.resolve(relPath)
        }
        .toMap

      writeFilesToSource(storage, localFiles)

      val inferredMeta = ModelFetcher.fetch(storage, unpackDir.toString)
      val contract = upload.contract.getOrElse(inferredMeta.contract).copy(modelName = modelName)
      val modelType = upload.modelType.map(ModelType.fromTag).getOrElse(inferredMeta.modelType)
      Result.okF(
        StorageUploadResult(
          name = modelName,
          modelType = modelType,
          description = upload.description,
          modelContract = contract
        )
      )
    } catch {
      case NonFatal(e) => Result.internalErrorF(e)
    }
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
      newPath = storage.rootDir.resolve(newFolder)
      result <- EitherT(moveFolder(oldPath, newPath))
    } yield result
    f.value
  }

  private def writeFilesToSource(source: ModelStorage, files: Map[Path, Path]): Unit = {
    files.foreach {
      case (src, dest) =>
        source.writeFile(dest.toString, src.toFile)
    }
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

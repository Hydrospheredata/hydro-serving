package io.hydrosphere.serving.manager.service.source.storages.local

import java.io.File
import java.nio.file._

import io.hydrosphere.serving.manager.service.source.storages.ModelStorage
import io.hydrosphere.serving.manager.util.FileUtils._
import io.hydrosphere.serving.model.api.{HResult, Result}
import org.apache.commons.io.FileUtils
import scala.util.Try

class LocalModelStorage(val sourceDef: LocalModelStorageDefinition) extends ModelStorage {
  val rootDir = sourceDef.path

  override def getReadableFile(path: String): HResult[File] = {
    val requestedPath = rootDir.resolve(path)
    if (Files.exists(requestedPath)) {
      Result.ok(requestedPath.toFile)
    } else {
      Result.clientError(s"$path doesn't exist in ${sourceDef.name} source")
    }
  }

  override def getSubDirs(path: String): HResult[List[String]] = {
    getReadableFile(path).right.map { dirPath =>
      dirPath
        .getSubDirectories
        .map(_.getName)
        .toList
    }
  }

  override def getAllFiles(path: String): HResult[List[String]] = {
    getReadableFile(path).right.map { file =>
      val fullUri = file.toURI
      file.listFilesRecursively
        .map(p => fullUri.relativize(p.toURI).toString)
        .toList
    }
  }

  override def exists(path: String): Boolean = {
    exists(Paths.get(path))
  }

  def exists(path: Path): Boolean = {
    val requestedPath = rootDir.resolve(path)
    Files.exists(requestedPath)
  }


  override def writeFile(path: String, localFile: File): HResult[Path] = {
    val destFile = rootDir.resolve(path)
    val destPath = destFile
    val parentPath = destPath.getParent
    if (!Files.exists(parentPath)) {
      Files.createDirectories(parentPath)
    }
    Result.ok(Files.copy(localFile.toPath, destPath, StandardCopyOption.REPLACE_EXISTING))
  }

  def removeFolder(path: Path): HResult[Unit] = {
    Try {
      FileUtils.deleteDirectory(rootDir.resolve(path).toFile)
    }.toEither.left.map(Result.InternalError(_))
  }
}
package io.hydrosphere.serving.manager.infrastructure.storage

import java.io.File
import java.nio.file._

import io.hydrosphere.serving.manager.util.FileUtils._
import io.hydrosphere.serving.model.api.{HResult, Result}
import org.apache.commons.io.FileUtils

import scala.util.Try

class LocalModelStorage(val rootPath: Path) extends ModelStorage {

  override def getReadableFile(path: String): HResult[File] = {
    val requestedPath = rootPath.resolve(path)
    if (Files.exists(requestedPath)) {
      Result.ok(requestedPath.toFile)
    } else {
      Result.clientError(s"$path doesn't exist in ${path.toString} folder")
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
    val requestedPath = path.resolve(path)
    Files.exists(requestedPath)
  }


  override def writeFile(target: String, source: File): HResult[Path] = {
    val destFile = rootPath.resolve(target)
    val destPath = destFile
    val parentPath = destPath.getParent
    if (!Files.exists(parentPath)) {
      Files.createDirectories(parentPath)
    }
    Result.ok(Files.copy(source.toPath, destPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES))
  }

  def removeFolder(path: Path): HResult[Unit] = {
    Try {
      FileUtils.deleteDirectory(path.resolve(path).toFile)
    }.toEither.left.map(Result.InternalError(_))
  }
}
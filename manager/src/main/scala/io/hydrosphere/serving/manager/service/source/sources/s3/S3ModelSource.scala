package io.hydrosphere.serving.manager.service.source.sources.s3

import java.io.File
import java.net.URI
import java.nio.file._

import com.amazonaws.{AmazonServiceException, SdkClientException}
import io.hydrosphere.serving.manager.model.{HResult, Result}
import io.hydrosphere.serving.manager.service.source.sources.ModelSource
import io.hydrosphere.serving.manager.service.source.sources.local._
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._

class S3ModelSource(val sourceDef: S3SourceDef) extends ModelSource with Logging {
  private[this] val lf = Files.createTempDirectory(sourceDef.name)
  private[this] val localSource = new LocalModelSource(
    LocalSourceDef(s"s3-proxy-${sourceDef.name}", Some(lf.toString))
  )

  private val client = sourceDef.s3Client

  override def getSubDirs(path: String): HResult[List[String]] = {
    logger.debug(s"getSubDirs: $path")

    val subdirs = client
      .listObjects(sourceDef.bucket)
      .getObjectSummaries
      .map(_.getKey)
      .filter(_.startsWith(path))
      .map(_.split(path).last.split("/"))
      .filterNot(_.length == 1)
      .map(_ (1))
      .distinct.toList

    Result.ok(subdirs)
  }

  override def getAllFiles(folder: String): HResult[List[String]] = {
    logger.debug(s"getAllFiles: $folder")
    val modelKeys = client
      .listObjects(sourceDef.bucket, folder)
      .getObjectSummaries
      .map(_.getKey)
      .filterNot(_.endsWith("/"))
    logger.debug(s"modelKeys=$modelKeys")
    modelKeys
      .map(URI.create)
      .map(_.toString)
      .map(getReadableFile)

    val r = localSource.getAllFiles(folder)
    logger.debug(s"getAllFiles=$r")
    r
  }

  override def getReadableFile(path: String): HResult[File] = {
    logger.debug(s"getReadableFile: $path")
    if (client.doesObjectExist(sourceDef.bucket, path)) {
      downloadObject(path.toString)
    } else if (client.listObjects(sourceDef.bucket, path).getObjectSummaries.nonEmpty) {
      downloadPrefix(path)
    } else {
      Result.clientError(s"File $path doesn't exist in ${sourceDef.name}")
    }
  }

  override def exists(path: String): Boolean = {
    if (client.doesObjectExist(sourceDef.bucket, path)) {
      true
    } else {
      client.listObjects(sourceDef.bucket, path).getObjectSummaries.nonEmpty
    }
  }

  override def writeFile(path: String, localFile: File): HResult[Path] = {
    try {
      val res = client.putObject(sourceDef.bucket, path, localFile)
      logger.debug(res)
      localSource.writeFile(path, localFile)
    } catch {
      case sdkEx: SdkClientException => Result.internalError(sdkEx)
      case serviceEx: AmazonServiceException => Result.internalError(serviceEx)
    }
  }

  private def downloadPrefix(prefix: String): HResult[File] = {
    logger.debug(s"downloadPrefix: $prefix")
    val objects = client.listObjects(sourceDef.bucket, prefix)
    if (objects.getObjectSummaries.nonEmpty) {
      val folderStructure = lf.resolve(prefix)

      if (Files.isRegularFile(folderStructure)) { // FIX sometimes s3 puts the entire folder
        Files.delete(folderStructure)
      }
      Files.createDirectories(folderStructure)

      getAllFiles(prefix).right.map { _ =>
        folderStructure.toFile
      }
    } else {
      Result.clientError(s"No objects in '$prefix' path exist in ${sourceDef.name}")
    }
  }

  private def downloadObject(objectPath: String): HResult[File] = {
    logger.debug(s"downloadObject: $objectPath")

    if (client.doesObjectExist(sourceDef.bucket, objectPath)) {
      val fileStream = client.getObject(sourceDef.bucket, objectPath).getObjectContent

      val folderStructure = lf.resolve(objectPath.split("/").dropRight(1).mkString("/"))

      if (Files.isRegularFile(folderStructure)) { // FIX sometimes s3 puts the entire folder
        Files.delete(folderStructure)
      }

      Files.createDirectories(folderStructure)

      val file = Files.createFile(lf.resolve(objectPath))
      Files.copy(fileStream, file, StandardCopyOption.REPLACE_EXISTING)

      Result.ok(file.toFile)
    } else {
      Result.clientError(s"File $objectPath doesn't exist in ${sourceDef.name}")
    }
  }
}

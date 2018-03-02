package io.hydrosphere.serving.manager.service.modelsource.s3

import java.io.{File, FileNotFoundException, IOException}
import java.net.URI
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.collection.JavaConversions._
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.service.modelsource.local.{LocalModelSource, LocalSourceDef}
import io.hydrosphere.serving.manager.service.modelsource.s3.S3ModelSource.RecursiveRemover
import org.apache.logging.log4j.scala.Logging

class S3ModelSource(val sourceDef: S3SourceDef) extends ModelSource with Logging {
  private[this] val localFolder = s"/tmp/${sourceDef.name}"
  private val client = sourceDef.s3Client

  val cacheSource = new LocalModelSource(
    LocalSourceDef(s"s3-proxy-${sourceDef.name}", localFolder)
  )

  val proxyFolder: Path = Paths.get(localFolder)

  def deleteProxyObject(objectPath: String): Boolean = {
    val path = Paths.get(localFolder, objectPath)
    if (Files.exists(path) && Files.isDirectory(path)) {
      Files.walkFileTree(path, new RecursiveRemover())
    }
    Files.deleteIfExists(path)
  }

  def downloadObject(objectPath: String): File = {
    logger.debug(s"downloadObject: $objectPath")
    if (!client.doesObjectExist(sourceDef.bucket, objectPath))
      throw new FileNotFoundException(objectPath)
    val fileStream = client.getObject(sourceDef.bucket, objectPath).getObjectContent

    val folderStructure = Paths.get(localFolder, objectPath.split("/").dropRight(1).mkString("/"))

    if (Files.isRegularFile(folderStructure)) { // FIX sometimes s3 puts the entire folder
      Files.delete(folderStructure)
    }

    Files.createDirectories(folderStructure)

    val file = Files.createFile(Paths.get(localFolder, objectPath))
    Files.copy(fileStream, file, StandardCopyOption.REPLACE_EXISTING)
    file.toFile
  }

  override def getSubDirs(path: String): List[String] = {
    logger.debug(s"getSubDirs: $path")
    client
      .listObjects(sourceDef.bucket)
      .getObjectSummaries
      .map(_.getKey)
      .filter(_.startsWith(path))
      .map(_.split(path).last.split("/"))
      .filterNot(_.length == 1)
      .map(_ (1))
      .distinct.toList
  }

  override def getSubDirs: List[String] = {
    val r = client
      .listObjects(sourceDef.bucket)
      .getObjectSummaries
      .map(_.getKey)
      .map(_.split("/").head)
      .distinct
      .toList
    logger.debug(s"getSubDirs $r")
    r
  }

  override def getAllFiles(folder: String): List[String] = {
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

    val r = cacheSource.getAllFiles(folder)
    logger.debug(s"getAllFiles=$r")
    r
  }

  def getAllCachedFiles(folder: String): List[String] = {
    cacheSource.getAllFiles(folder)
  }

  override def getReadableFile(path: String): File = {
    logger.debug(s"getReadableFile: $path")

    val fullObjectPath = URI.create(s"$path")
    val file = cacheSource.getReadableFile(path)
    if (!file.exists()) downloadObject(fullObjectPath.toString)
    file
  }

  override def getAbsolutePath(modelSource: String): Path = {
    logger.debug(s"getAbsolutePath: $modelSource")
    Paths.get(localFolder, modelSource)
  }

  override def isExist(path: String): Boolean = {
    if (client.doesObjectExist(sourceDef.bucket, path)) {
      true
    } else {
      client.listObjects(sourceDef.bucket, path).getObjectSummaries.nonEmpty
    }
  }

  override def writeFile(path: String, localFile: File): Unit = {
    client.putObject(sourceDef.bucket, path, localFile)
  }
}

object S3ModelSource {

  class RecursiveRemover extends FileVisitor[Path] with Logging {
    def visitFileFailed(file: Path, exc: IOException) = {
      logger.warn(s"Error while visiting file: ${exc.getMessage}")
      FileVisitResult.CONTINUE
    }

    def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      Files.delete(file)
      FileVisitResult.CONTINUE
    }

    def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE

    def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
      Option(exc).foreach { ex =>
        logger.warn(s"Error in post visiting directory: ${ex.getMessage}")
      }
      Files.delete(dir)
      FileVisitResult.CONTINUE
    }
  }

}
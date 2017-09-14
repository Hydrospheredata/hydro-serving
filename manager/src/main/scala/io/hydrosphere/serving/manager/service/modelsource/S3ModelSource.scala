package io.hydrosphere.serving.manager.service.modelsource

import java.io.{File, FileNotFoundException, IOException}
import java.net.URI
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, S3ModelSourceConfiguration}
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._

/**
  * Created by Bulat on 31.05.2017.
  */
class S3ModelSource(val configuration: S3ModelSourceConfiguration) extends ModelSource with Logging {
  private[this] val localFolder =  s"/tmp/${configuration.name}"
  val cacheSource = new LocalModelSource(
    LocalModelSourceConfiguration(s"proxy-${configuration.name}", localFolder)
  )

  val client = configuration.s3Client

  val proxyFolder: Path = Paths.get(localFolder)

  def deleteProxyObject(objectPath: String): Boolean = {
    val path = Paths.get(localFolder, objectPath)
    if (Files.exists(path) && Files.isDirectory(path)) {
      Files.walkFileTree(path, new FileVisitor[Path] {
        def visitFileFailed(file: Path, exc: IOException) = FileVisitResult.CONTINUE
        def visitFile(file: Path, attrs: BasicFileAttributes) = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }
        def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE
        def postVisitDirectory(dir: Path, exc: IOException) = {
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      })
    }
    Files.deleteIfExists(path)
  }

  def downloadObject(objectPath: String): File = {
    logger.debug(s"downloadObject: $objectPath")
    if (!client.doesObjectExist(configuration.bucket, objectPath))
      throw new FileNotFoundException(objectPath)
    val fileStream = client.getObject(configuration.bucket, objectPath).getObjectContent

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
      .listObjects(configuration.bucket)
      .getObjectSummaries
      .map(_.getKey)
      .filter(_.startsWith(path))
      .map(_.split(path).last.split("/"))
      .filterNot(_.length == 1)
      .map(_(1))
      .distinct.toList
  }

  override def getSubDirs: List[String] = {
    logger.debug(s"getSubDirs")
    val r = client
      .listObjects(configuration.bucket)
      .getObjectSummaries
      .map(_.getKey)
      .map(_.split("/").head)
      .distinct
      .toList
    println(r)
    r
  }

  override def getAllFiles(folder: String): List[String] = {
    logger.debug(s"getAllFiles: $folder")
    val modelKeys = client
      .listObjects(configuration.bucket, folder)
      .getObjectSummaries
      .map(_.getKey)
      .filterNot(_.endsWith("/")) // fix w2v/stages/0_w2v_617dd94f64cf/data/
    println(modelKeys)
    modelKeys
      .map(URI.create)
      .map(_.toString)
      .map(getReadableFile)

    val r = cacheSource.getAllFiles(folder)
    println(r)
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

  override def getSourcePrefix: String = configuration.name

  override def getAbsolutePath(modelSource: String): Path = {
    logger.debug(s"getAbsolutePath: $modelSource")
    Paths.get(localFolder, modelSource)
  }

  override def isExist(path: String): Boolean = {
    if (client.doesObjectExist(configuration.bucket, path)) {
      true
    } else {
      client.listObjects(configuration.bucket, path).getObjectSummaries.nonEmpty
    }
  }
}

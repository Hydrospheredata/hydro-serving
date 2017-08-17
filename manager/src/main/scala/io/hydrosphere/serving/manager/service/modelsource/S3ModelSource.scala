package io.hydrosphere.serving.manager.service.modelsource

import java.io.{File, FileNotFoundException}
import java.net.URI
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, S3ModelSourceConfiguration}
import org.apache.logging.log4j.scala.Logging
import scala.collection.JavaConversions._

/**
  * Created by Bulat on 31.05.2017.
  */
class S3ModelSource(val configuration: S3ModelSourceConfiguration) extends ModelSource with Logging {
  private[this] val localFolder =  s"/tmp/${configuration.name}"
  private[this] val fileCache = new LocalModelSource(
    LocalModelSourceConfiguration(s"proxy-${configuration.name}", localFolder)
  )

  val client = configuration.s3Client

  val proxyFolder: Path = Paths.get(localFolder)

  def deleteProxyObject(objectPath: String): Boolean = {
    Files.deleteIfExists(Paths.get(localFolder, objectPath))
  }

  def downloadObject(objectPath: String): File = {
    logger.debug(s"downloadObject: $objectPath")
    if (!client.doesObjectExist(configuration.bucket, objectPath))
      throw new FileNotFoundException(objectPath)
    val fileStream = client.getObject(configuration.bucket, objectPath).getObjectContent

    val folderStructure = Paths.get(localFolder, objectPath.split("/").dropRight(1).mkString("/"))

    Files.createDirectories(folderStructure)

    val file = Files.createFile(Paths.get(localFolder, objectPath))
    Files.copy(fileStream, file, StandardCopyOption.REPLACE_EXISTING)
    file.toFile
  }

  override def getSubDirs(path: String): List[String] = {
    logger.debug(s"getSubDirs: $path")
    val pUri = Paths.get(path).toUri
    client
      .listObjects(configuration.bucket)
      .getObjectSummaries
      .map(_.getKey)
      .map(Paths.get(_).toUri)
      .map(k => pUri.relativize(k).toString.split("/").head)
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

    val r = fileCache.getAllFiles(folder)
    println(r)
    r
  }

  override def getReadableFile(path: String): File = {
    logger.debug(s"getReadableFile: $path")

    val fullObjectPath = URI.create(s"$path")
    val file = fileCache.getReadableFile(path)
    if (!file.exists()) downloadObject(fullObjectPath.toString)
    file
  }

  override def getSourcePrefix: String = configuration.name

  override def getAbsolutePath(modelSource: String): Path = {
    logger.debug(s"getAbsolutePath: $modelSource")
    Paths.get(localFolder, modelSource)
  }
}

package io.hydrosphere.serving.manager.service.modelsource

import java.io.{File, FileNotFoundException}
import java.net.URI
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import awscala.s3.S3
import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, S3ModelSourceConfiguration}
import org.apache.logging.log4j.scala.Logging

import scala.collection.concurrent.TrieMap

/**
  * Created by Bulat on 31.05.2017.
  */
class S3ModelSource(val configuration: S3ModelSourceConfiguration) extends ModelSource with Logging {
  private[this] val localFolder =  s"/tmp/${configuration.name}"
  private[this] val fileCache = new LocalModelSource(
    LocalModelSourceConfiguration(s"proxy-${configuration.name}", localFolder)
  )
  private[this] implicit val s3 = S3.at(configuration.region)
  private[this] val bucketObj = s3.bucket(configuration.bucket).get

  private def downloadObject(objectPath: String): File = {
    logger.debug(s"downloadObject: $objectPath")
    val fileStream = bucketObj.getObject(objectPath).getOrElse(throw new FileNotFoundException(objectPath)).content

    val folderStructure = Paths.get(localFolder, objectPath.split("/").dropRight(1).mkString("/"))
    Files.createDirectories(folderStructure)

    val file = Files.createFile(Paths.get(localFolder, objectPath))
    Files.copy(fileStream, file, StandardCopyOption.REPLACE_EXISTING)
    val f = file.toFile
    println(f)
    f
  }

  override def getSubDirs(path: String): List[String] = {
    logger.debug(s"getSubDirs: $path")
    val pUri = Paths.get(path).toUri
    bucketObj.keys(path)
      .map(Paths.get(_).toUri)
      .map(k => pUri.relativize(k).toString.split("/").head)
      .distinct.toList
  }

  override def getSubDirs: List[String] = {
    logger.debug(s"getSubDirs")
    bucketObj.keys().map(_.split("/").head).distinct.toList
  }

  override def getAllFiles(folder: String): List[String] = {
    logger.debug(s"getAllFiles: $folder")
    val pUri = URI.create(s"$folder")
    val modelKeys = bucketObj.keys(pUri.toString).toList
    modelKeys
      .map(k => URI.create(k))
      .map(k => pUri.relativize(k))
      .map(k => getReadableFile(k.toString))

    fileCache.getAllFiles(folder)
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
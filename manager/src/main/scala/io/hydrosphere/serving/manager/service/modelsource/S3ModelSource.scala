package io.hydrosphere.serving.manager.service.modelsource

import java.io.File
import java.net.URI
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import awscala.Region
import awscala.s3.S3
import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, S3ModelSourceConfiguration}

import scala.collection.concurrent.TrieMap

/**
  * Created by Bulat on 31.05.2017.
  */
class S3ModelSource(val configuration: S3ModelSourceConfiguration) extends ModelSource {
  private [this] val localFolder =  s"/tmp/${configuration.name}"
  private[this] val fileCache = new LocalModelSource(
    LocalModelSourceConfiguration(s"proxy-${configuration.name}", localFolder)
  )
  private[this] implicit val s3 = S3.at(Region(configuration.region.getName))
  private[this] val bucketObj = s3.bucket(configuration.bucket).get

  private def downloadObject(objectPath: String): File = {
    val fileStream = bucketObj.getObject(objectPath).get.content
    Files.createDirectories(Paths.get(localFolder, objectPath.split("/").dropRight(1).mkString))
    val file = Files.createFile(Paths.get(localFolder, objectPath))
    Files.copy(fileStream, file, StandardCopyOption.REPLACE_EXISTING)
    file.toFile
  }

  override def getSubDirs(path: String): List[String] = {
    val pUri = Paths.get(path).toUri
    val r = bucketObj.keys(path)
      .map(Paths.get(_).toUri)
      .map(k => pUri.relativize(k).toString.split("/").head)
      .distinct.toList
    r
  }

  override def getSubDirs: List[String] = {
    val r = bucketObj.keys().map(_.split("/").head).distinct.toList
    r
  }

  override def getAllFiles(folder: String): List[String] = {
    val pUri = URI.create(s"$folder")
    val modelKeys = bucketObj.keys(pUri.toString).toList
    modelKeys
      .map(k => URI.create(k))
      .map(k => pUri.relativize(k))
      .map(k => getReadableFile(k.toString))

    fileCache.getAllFiles(folder)
  }

  override def getReadableFile(path: String): File = {
    val fullObjectPath = URI.create(s"$path")
    println(fullObjectPath)
    val file = fileCache.getReadableFile(path)
    if (!file.exists()) downloadObject(fullObjectPath.toString)
    file
  }

  override def getSourcePrefix(): String = configuration.name

  override def getModelPath(modelSource: String): Path = ???

  override def getLocalCopy(source: String): Path = ???
}
package io.hydrosphere.serving.manager.service.modelsource

import java.io.File
import java.net.URI
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import awscala.s3.S3
import io.hydrosphere.serving.manager.S3ModelSourceConfiguration

import scala.collection.concurrent.TrieMap

/**
  * Created by Bulat on 31.05.2017.
  */
class S3ModelSource(val conf: S3ModelSourceConfiguration) extends ModelSource {
  private[this] val fileCache = TrieMap.empty[String, TrieMap[String, File]]
  private[this] implicit val s3 = S3.at(conf.region)
  private[this] val bucketObj = s3.bucket(conf.bucket).get

  private def downloadObject(objectPath: String): File = {
    val fileStream = bucketObj.getObject(objectPath).get.content
    val file = Files.createTempFile("s3cache-", objectPath.split("/").last)
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

  override def getAllFiles(runtimeName: String, modelName: String): List[String] = {
    val pUri = URI.create(s"$runtimeName/$modelName")
    val modelKeys = bucketObj.keys(pUri.toString).toList
    println(s"$pUri: $modelKeys")
    modelKeys
      .map(k => URI.create(k))
      .map(k => pUri.relativize(k))
      .map(k => getReadableFile(runtimeName, modelName, k.toString))

    fileCache(modelName).keys
      .map(k => URI.create(k))
      .map(k => pUri.relativize(k))
      .map(k => k)
      .map(_.toString)
      .toList
  }

  override def getReadableFile(runtimeName: String, modelName: String, path: String): File = {
    val fullObjectPath = URI.create(s"$runtimeName/$modelName/$path")
    println(fullObjectPath)
    val subCache = fileCache.getOrElseUpdate(modelName, TrieMap.empty)
    subCache.getOrElseUpdate(path, downloadObject(fullObjectPath.toString))
  }

  override def getSourcePrefix(): String = conf.name

  override def getLocalCopy(source: String): Path = ???
}
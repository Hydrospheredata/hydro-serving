package io.hydrosphere.serving.manager.service

import java.nio.file.Path

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.ManagerConfiguration
import io.hydrosphere.serving.manager.controller.model_source.{AddLocalSourceRequest, AddS3SourceRequest}
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelMetadata
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.{LocalSourceParams, S3SourceParams}
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

case class SourcePath(sourceName: String, path: String)

object SourcePath {
  def parse(source: String): HResult[SourcePath] = {
    val args = source.split(':')
    if (args.length == 2) {
      for {
        sourceName <- args.headOption.toHResult(ClientError("Source name is not defined")).right
        path <- args.lastOption.toHResult(ClientError("Source path is not defined")).right
      } yield SourcePath(sourceName, path)
    } else {
      Result.clientError("Incorrect source path")
    }
  }
}

trait SourceManagementService {
  /***
    * Try to add a S3 source
    * @param r config that defines a S3 source
    * @return config representation from db
    */
  def addS3Source(r: AddS3SourceRequest): HFResult[ModelSourceConfig]

  /***
    * Try to add a local source
    * @param r config that defines a local source
    * @return config representation from db
    */
  def addLocalSource(r: AddLocalSourceRequest): HFResult[ModelSourceConfig]

  /***
    * Try to fetch a model metadata from specified source path
    * @param source string which describes a path inside a source
    * @return If path doesn't exist - None. Some metadata otherwise
    */
  def index(source: String): HFResult[Option[ModelMetadata]]

  /***
    * Try to add a source
    * @param modelSourceConfigAux config that defines a source
    * @return config representation from db
    */
  def addSource(modelSourceConfigAux: ModelSourceConfig): HFResult[ModelSourceConfig]

  /***
    * List all the sources (db ++ config)
    * @return List of all sources
    */
  def getSources: Future[List[ModelSource]]

  /***
    * Get readable path from url
    * @param url in-source url that defines some resource
    * @return readable path object
    */
  def getLocalPath(url: String): HFResult[Path]

  /***
    * List all the source configs (db ++ config)
    * @return List of all sources configs
    */
  def allSourceConfigs: Future[Seq[ModelSourceConfig]]

  /***
    * Try to get source by name
    * @param name name of a source
    * @return source
    */
  def getSource(name: String): HFResult[ModelSource]
}

class SourceManagementServiceImpl(
  managerConfiguration: ManagerConfiguration,
  sourceRepository: SourceConfigRepository)
  (implicit ex: ExecutionContext) extends SourceManagementService with Logging {

  def addSource(modelSourceConfigAux: ModelSourceConfig): HFResult[ModelSourceConfig] = {
    getSourceConfig(modelSourceConfigAux.name).flatMap {
      case Right(_) => Result.clientErrorF(s"ModelSource with name ${modelSourceConfigAux.name} already exists")
      case Left(ClientError(_)) => // consider more specific NotFound error?
        val modelSource = ModelSource.fromConfig(modelSourceConfigAux)
        for {
          config <- sourceRepository.create(modelSourceConfigAux)
        } yield {
          Right(config)
        }
      case Left(x) => Result.errorF(x)
    }
  }

  def getSourceConfig(name: String): HFResult[ModelSourceConfig] = {
    allSourceConfigs.map { sources =>
      sources.find(_.name == name)
        .map(Right.apply)
        .getOrElse(Result.clientError(s"Can't find a '$name' source"))
    }
  }

  override def getSources: Future[List[ModelSource]] = {
    allSourceConfigs.map { sources =>
      sources.map(ModelSource.fromConfig).toList
    }
  }

  override def getLocalPath(url: String): HFResult[Path] = {
    val args = url.split(':')
    val source = args.head
    val path = args.last
    getSources.map {
      _.find(_.sourceDef.name == source)
        .map(_.getAbsolutePath(path))
        .map(Right.apply)
        .getOrElse(Result.clientError(s"ModelSource for $url with prefix $source is not found"))
    }
  }

  override def addLocalSource(r: AddLocalSourceRequest): HFResult[ModelSourceConfig] = {
    val config = ModelSourceConfig(
      id = -1,
      name = r.name,
      params = LocalSourceParams(Some(r.path))
    )
    addSource(config)
  }

  override def addS3Source(r: AddS3SourceRequest): HFResult[ModelSourceConfig] = {
    val config = ModelSourceConfig(
      id = -1,
      name = r.name,
      params = S3SourceParams(
        awsAuth = r.key,
        bucketName = r.bucket,
        path = r.path,
        region = r.region
      )
    )
    addSource(config)
  }

  override def allSourceConfigs: Future[Seq[ModelSourceConfig]] = {
    sourceRepository.all().map { dbSources =>
      managerConfiguration.modelSources ++ dbSources
    }
  }

  override def getSource(name: String): HFResult[ModelSource] = {
    getSourceConfig(name).map { res =>
      res.right.map { config =>
        ModelSource.fromConfig(config)
      }
    }
  }

  override def index(modelSource: String): HFResult[Option[ModelMetadata]] = {
    val f = for {
      sourcePath <- EitherT(Future.successful(SourcePath.parse(modelSource)))
      source <- EitherT(getSource(sourcePath.sourceName))
    } yield {
      println(sourcePath)
      if (source.isExist(sourcePath.path)) {
        Some(ModelFetcher.fetch(source, sourcePath.path))
      } else {
        None
      }
    }
    f.value
  }

}
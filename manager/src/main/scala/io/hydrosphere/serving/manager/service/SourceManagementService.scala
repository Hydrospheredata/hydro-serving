package io.hydrosphere.serving.manager.service

import java.nio.file.Path

import io.hydrosphere.serving.manager.ManagerConfiguration
import io.hydrosphere.serving.manager.controller.model_source.{AddLocalSourceRequest, AddS3SourceRequest}
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelMetadata
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.{LocalSourceParams, S3SourceParams}
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class SourcePath(sourceName: String, path: String)

object SourcePath {
  def parse(source: String): Option[SourcePath] = {
    val args = source.split(':')
    if (args.length == 2) {
      for {
        sourceName <- args.headOption
        path <- args.lastOption
      } yield SourcePath(sourceName, path)
    } else {
      None
    }
  }

  def parseOrEx(source: String): SourcePath = {
    parse(source).getOrElse(throw new IllegalArgumentException(s"Invalid source: $source"))
  }
}

trait SourceManagementService {
  def index(source: String): Future[Try[Option[ModelMetadata]]]

  def addS3Source(r: AddS3SourceRequest): Future[Option[ModelSourceConfig]]

  def addLocalSource(r: AddLocalSourceRequest): Future[Option[ModelSourceConfig]]

  def addSource(modelSourceConfigAux: ModelSourceConfig): Future[Option[ModelSourceConfig]]

  def getSources: Future[List[ModelSource]]

  def getLocalPath(url: String): Future[Path]

  def allSourceConfigs: Future[Seq[ModelSourceConfig]]

  def getSource(name: String): Future[Option[ModelSource]]
}

class SourceManagementServiceImpl(
  managerConfiguration: ManagerConfiguration,
  sourceRepository: SourceConfigRepository)
  (implicit ex: ExecutionContext) extends SourceManagementService with Logging {

  def addSource(modelSourceConfigAux: ModelSourceConfig): Future[Option[ModelSourceConfig]] = {
    getSourceConfig(modelSourceConfigAux.name).flatMap {
      case Some(_) => Future.successful(None)
      case None =>
        sourceRepository.create(modelSourceConfigAux).map(Some.apply)
    }
  }

  def getSourceConfig(name: String): Future[Option[ModelSourceConfig]] = {
    allSourceConfigs.map { sources => sources.find(_.name == name) }
  }

  override def getSources: Future[List[ModelSource]] = {
    allSourceConfigs.map { sources =>
      sources.map(ModelSource.fromConfig).toList
    }
  }

  override def getLocalPath(url: String): Future[Path] = {
    val sourcePath = SourcePath.parseOrEx(url)
    getSources.map {
      _.find(_.sourceDef.name == sourcePath.sourceName)
        .map(_.getAbsolutePath(sourcePath.path))
        .getOrElse(throw new IllegalArgumentException(s"ModelSource for $url with prefix ${sourcePath.sourceName} is not found"))
    }
  }

  override def addLocalSource(r: AddLocalSourceRequest): Future[Option[ModelSourceConfig]] = {
    val config = ModelSourceConfig(
      id = -1,
      name = r.name,
      params = LocalSourceParams(Some(r.path))
    )
    addSource(config)
  }

  override def addS3Source(r: AddS3SourceRequest): Future[Option[ModelSourceConfig]] = {
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

  override def getSource(name: String): Future[Option[ModelSource]] = {
    allSourceConfigs.map { configs =>
      configs.find(_.name == name).map(ModelSource.fromConfig)
    }
  }

  override def index(modelSource: String): Future[Try[Option[ModelMetadata]]] = {
    val sourcePath = SourcePath.parseOrEx(modelSource)
    getSource(sourcePath.sourceName).map {
      case Some(source) =>
        if (source.isExist(sourcePath.path)) {
          Success(Some(ModelFetcher.fetch(source, sourcePath.path)))
        } else {
          Success(None)
        }
      case None =>
        Failure(new IllegalArgumentException(s"Cant find ModelSource for $modelSource"))
    }
  }
}
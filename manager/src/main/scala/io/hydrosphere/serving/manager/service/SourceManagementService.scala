package io.hydrosphere.serving.manager.service

import java.nio.file.Path

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
  def addS3Source(r: AddS3SourceRequest): HFResult[ModelSourceConfigAux]
  def index(source: String): Future[Try[Option[ModelMetadata]]]

  def addLocalSource(r: AddLocalSourceRequest): HFResult[ModelSourceConfigAux]

  def addSource(modelSourceConfigAux: ModelSourceConfigAux): HFResult[ModelSourceConfigAux]

  def getSources: Future[List[ModelSource]]

  def getLocalPath(url: String): HFResult[Path]

  def allSourceConfigs: Future[Seq[ModelSourceConfig]]

  def getSource(name: String): HFResult[ModelSource]
}

class SourceManagementServiceImpl(
  managerConfiguration: ManagerConfiguration,
  sourceRepository: SourceConfigRepository)
  (implicit ex: ExecutionContext) extends SourceManagementService with Logging {

  private val watcherRegistry = actorSystem.actorOf(WatcherRegistryActor.props, "WatcherRegistry")

  def addSource(modelSourceConfigAux: ModelSourceConfigAux): HFResult[ModelSourceConfigAux] = {
    getSourceConfig(modelSourceConfigAux.name).flatMap {
      case Right(_) => Result.clientErrorF(s"ModelSource with name ${modelSourceConfigAux.name} already exists")
      case Left(ClientError(_)) => // consider more specific NotFound error?
        val modelSource = ModelSource.fromConfig(modelSourceConfigAux)
        for {
          config <- sourceRepository.create(modelSourceConfigAux)
          _ <- createWatcher(modelSource)
        } yield {
          Right(config)
        }
      case Left(x) => Result.errorF(x)
    }
  }

  def getSourceConfig(name: String): HFResult[ModelSourceConfigAux] = {
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

  override def createWatchers: Future[Seq[ActorRef]] = {
    getSources.flatMap { sources =>
      val watchers = sources.map(createWatcher)
      Future.sequence(watchers)
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

  override def addLocalSource(r: AddLocalSourceRequest): Future[Option[ModelSourceConfig]] = {
    val config = ModelSourceConfig(
      id = -1,
      name = r.name,
      params = LocalSourceParams(Some(r.path))
    )
    addSource(config)
  }

  override def addS3Source(r: AddS3SourceRequest): HFResult[ModelSourceConfigAux] = {
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

  override def addLocalSource(r: AddLocalSourceRequest): HFResult[ModelSourceConfigAux] = {
    val config = ModelSourceConfig(
      id = -1,
      name = r.name,
      params = LocalSourceParams(
        path = r.path
      )
    ).toAux
    addSource(config)
  }

  override def allSourceConfigs: Future[Seq[ModelSourceConfigAux]] = {
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
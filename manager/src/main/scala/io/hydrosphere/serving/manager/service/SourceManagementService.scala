package io.hydrosphere.serving.manager.service

import java.nio.file.Path

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern._
import akka.util.Timeout
import io.hydrosphere.serving.manager.ManagerConfiguration
import io.hydrosphere.serving.manager.controller.model_source.{AddLocalSourceRequest, AddS3SourceRequest}
import io.hydrosphere.serving.manager.model.Result.{ClientError}
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.modelsource.WatcherRegistryActor.AddWatcher
import io.hydrosphere.serving.manager.service.modelsource.{ModelSource, WatcherRegistryActor}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

trait SourceManagementService {
  def addS3Source(r: AddS3SourceRequest): HFResult[ModelSourceConfigAux]

  def addLocalSource(r: AddLocalSourceRequest): HFResult[ModelSourceConfigAux]

  def addSource(modelSourceConfigAux: ModelSourceConfigAux): HFResult[ModelSourceConfigAux]

  def createWatcher(modelSource: ModelSource): Future[ActorRef]

  def getSources: Future[List[ModelSource]]

  def getLocalPath(url: String): HFResult[Path]

  def createWatchers: Future[Seq[ActorRef]]

  def allSourceConfigs: Future[Seq[ModelSourceConfigAux]]

  def getSource(name: String): HFResult[ModelSource]
}

class SourceManagementServiceImpl(managerConfiguration: ManagerConfiguration, sourceRepository: SourceConfigRepository)
  (implicit ex: ExecutionContext, actorSystem: ActorSystem, timeout: Timeout) extends SourceManagementService with Logging {

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

  def createWatcher(modelSourceConfigAux: ModelSourceConfigAux): Future[ActorRef] = {
    val modelSource = ModelSource.fromConfig(modelSourceConfigAux)
    createWatcher(modelSource)
  }

  def createWatcher(modelSource: ModelSource): Future[ActorRef] = {
    val watcher = watcherRegistry ? AddWatcher(modelSource)
    watcher.mapTo[ActorRef]
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

  override def addS3Source(r: AddS3SourceRequest): HFResult[ModelSourceConfigAux] = {
    val config = ModelSourceConfig(
      id = -1,
      name = r.name,
      params = S3SourceParams(
        awsAuth = r.key,
        bucketName = r.bucket,
        queueName = r.queue,
        region = r.region
      )
    ).toAux
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
}
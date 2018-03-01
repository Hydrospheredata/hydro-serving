package io.hydrosphere.serving.manager.service

import java.nio.file.Path

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern._
import akka.util.Timeout
import io.hydrosphere.serving.manager.ManagerConfiguration
import io.hydrosphere.serving.manager.controller.model_source.{AddLocalSourceRequest, AddS3SourceRequest}
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.modelsource.WatcherRegistryActor.AddWatcher
import io.hydrosphere.serving.manager.service.modelsource.{ModelSource, WatcherRegistryActor}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

trait SourceManagementService {
  def addS3Source(r: AddS3SourceRequest): Future[Option[ModelSourceConfigAux]]

  def addLocalSource(r: AddLocalSourceRequest): Future[Option[ModelSourceConfigAux]]

  def addSource(modelSourceConfigAux: ModelSourceConfigAux): Future[Option[ModelSourceConfigAux]]

  def createWatcher(modelSource: ModelSource): Future[ActorRef]

  def getSources: Future[List[ModelSource]]

  def getLocalPath(url: String): Future[Path]

  def createWatchers: Future[Seq[ActorRef]]

  def allSourceConfigs: Future[Seq[ModelSourceConfigAux]]

  def getSource(name: String): Future[Option[ModelSource]]
}

class SourceManagementServiceImpl(managerConfiguration: ManagerConfiguration, sourceRepository: SourceConfigRepository)
  (implicit ex: ExecutionContext, actorSystem: ActorSystem, timeout: Timeout) extends SourceManagementService with Logging {

  private val watcherRegistry = actorSystem.actorOf(WatcherRegistryActor.props, "WatcherRegistry")

  def addSource(modelSourceConfigAux: ModelSourceConfigAux): Future[Option[ModelSourceConfigAux]] = {
    getSourceConfig(modelSourceConfigAux.name).flatMap {
      case Some(_) => Future.successful(None)
      case None =>
        val modelSource = ModelSource.fromConfig(modelSourceConfigAux)
        for {
          config <- sourceRepository.create(modelSourceConfigAux)
          _ <- createWatcher(modelSource)
        } yield {
          Some(config)
        }
    }
  }

  def getSourceConfig(name: String): Future[Option[ModelSourceConfigAux]] = {
    allSourceConfigs.map { sources => sources.find(_.name == name) }
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

  override def getLocalPath(url: String): Future[Path] = {
    val args = url.split(':')
    val source = args.head
    val path = args.last
    getSources.map {
      _.find(_.sourceDef.name == source)
        .map(_.getAbsolutePath(path))
        .getOrElse(throw new IllegalArgumentException(s"ModelSource for $url with prefix $source is not found"))
    }
  }

  override def addS3Source(r: AddS3SourceRequest): Future[Option[ModelSourceConfigAux]] = {
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

  override def addLocalSource(r: AddLocalSourceRequest): Future[Option[ModelSourceConfigAux]] = {
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

  override def getSource(name: String): Future[Option[ModelSource]] = {
    sourceRepository.get(name).map { maybeSource =>
      maybeSource.map(ModelSource.fromConfig)
    }
  }
}
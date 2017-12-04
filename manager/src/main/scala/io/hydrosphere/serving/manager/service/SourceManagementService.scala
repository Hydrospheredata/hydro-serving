package io.hydrosphere.serving.manager.service

import java.nio.file.Path

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern._
import akka.util.Timeout
import io.hydrosphere.serving.manager.actor.WatcherRegistryActor
import io.hydrosphere.serving.manager.actor.WatcherRegistryActor.AddWatcher
import io.hydrosphere.serving.manager.model.{LocalSourceParams, ModelSourceConfigAux, S3SourceParams, SourceParams}
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

case class CreateModelSourceRequest(
  name: String,
  params: SourceParams
)

trait SourceManagementService {
  def addSource(createModelSourceRequest: CreateModelSourceRequest): Future[ModelSourceConfigAux]

  def addSource(modelSourceConfigAux: ModelSourceConfigAux): Future[ActorRef]

  def createWatcher(modelSource: ModelSource): Future[ActorRef]

  def getSources: Future[List[ModelSource]]

  def getSourceConfigs: Future[List[ModelSourceConfigAux]]

  def getLocalPath(url: String): Future[Path]

  def createWatchers: Future[Seq[ActorRef]]
}

class SourceManagementServiceImpl(sourceRepository: SourceConfigRepository)
  (implicit ex: ExecutionContext, actorSystem: ActorSystem, timeout: Timeout) extends SourceManagementService with Logging {

  val watcherRegistry = actorSystem.actorOf(WatcherRegistryActor.props, "WatcherRegistry")

  def addSource(createModelSourceRequest: CreateModelSourceRequest): Future[ModelSourceConfigAux] = {
    val config = ModelSourceConfigAux(-1, createModelSourceRequest.name, createModelSourceRequest.params)
    addSource(config).map(_ => config)
  }

  def addSource(modelSourceConfigAux: ModelSourceConfigAux): Future[ActorRef] = {
    val modelSource = ModelSource.fromConfig(modelSourceConfigAux)
    sourceRepository.create(modelSourceConfigAux)
    createWatcher(modelSource)
  }

  def createWatcher(modelSource: ModelSource): Future[ActorRef] = {
    val watcher = watcherRegistry ? AddWatcher(modelSource)
    watcher.mapTo[ActorRef]
  }

  def deleteSource(modelSourceConfigAux: ModelSourceConfigAux) = {
    ???
  }

  override def getSourceConfigs = {
    sourceRepository.all().map{
      _.map{ s =>
        s.params match {
          case x: LocalSourceParams => s
          case x: S3SourceParams =>
            ModelSourceConfigAux(
              s.id,
              s.name,
              x.copy(awsAuth = x.awsAuth.map(_.hide))
            )
        }

      }.toList
    }
  }

  override def getSources: Future[List[ModelSource]] = {
    sourceRepository.all().map {
      _.map { s =>
        ModelSource.fromConfig(s)
      }.toList
    }
  }

  override def createWatchers: Future[Seq[ActorRef]] = {
    getSources.flatMap { sources =>
      val watchers = sources.map { createWatcher }
      Future.sequence(watchers)
    }
  }

  override def getLocalPath(url: String): Future[Path] = {
    val args = url.split(':')
    val source = args.head
    val path = args.last
    getSources.map {
      _.find(_.sourceDef.prefix == source)
        .map(_.getAbsolutePath(path))
        .getOrElse(throw new IllegalArgumentException(s"ModelSource for $url with prefix $source is not found"))
    }
  }

}

object SourceManagementServiceImpl {

}
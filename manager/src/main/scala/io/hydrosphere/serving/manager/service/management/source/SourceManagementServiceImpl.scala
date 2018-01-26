package io.hydrosphere.serving.manager.service.management.source

import java.nio.file.Path

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern._
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.ModelSourceConfigAux
import io.hydrosphere.serving.manager.model.SourceParams.{LocalSourceParams, S3SourceParams}
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.modelsource.{ModelSource, WatcherRegistryActor}
import io.hydrosphere.serving.manager.service.modelsource.WatcherRegistryActor.AddWatcher
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class SourceManagementServiceImpl(sourceRepository: SourceConfigRepository)
  (implicit ex: ExecutionContext, actorSystem: ActorSystem, timeout: Timeout) extends SourceManagementService with Logging {

  private val watcherRegistry = actorSystem.actorOf(WatcherRegistryActor.props, "WatcherRegistry")

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

  /*def deleteSource(modelSourceConfigAux: ModelSourceConfigAux) = {
    ???
  }*/

  override def getSourceConfigs: Future[List[ModelSourceConfigAux]] = {
    sourceRepository.all().map {
      _.map { s =>
        s.params match {
          case _: LocalSourceParams => s
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
      val watchers = sources.map {
        createWatcher
      }
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

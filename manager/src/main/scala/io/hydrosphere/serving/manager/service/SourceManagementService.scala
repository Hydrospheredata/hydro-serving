package io.hydrosphere.serving.manager.service

import java.nio.file.Path

import akka.actor.{ActorRef, ActorSystem}
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

trait SourceManagementService {
  def getSources: Future[Seq[ModelSource]]

  def getLocalPath(url: String): Future[Path]

  def createWatchers(context: ActorSystem): Future[Seq[ActorRef]]
}

class SourceManagementServiceImpl(sourceRepository: SourceConfigRepository)
  (implicit ex: ExecutionContext) extends SourceManagementService with Logging {

  override def createWatchers(system: ActorSystem): Future[Seq[ActorRef]] =
    getSources.map(_.map { conf =>
      logger.info(s"SourceWatcher initialization: ${conf.configuration.name}")
      system.actorOf(SourceWatcher.props(conf), s"Watcher@${conf.configuration.name}")
    })

  override def getSources: Future[Seq[ModelSource]] =
    sourceRepository.all().map{ _.map(ModelSource.fromConfig)}

  override def getLocalPath(url: String): Future[Path] = {
    val args = url.split(':')
    val source = args.head
    val path = args.last
    getSources.map(
      _.find(_.getSourcePrefix == source)
      .map(_.getAbsolutePath(path))
      .getOrElse(throw new IllegalArgumentException(s"Can't find ModelSource for prefix $source"))
    )
  }
}
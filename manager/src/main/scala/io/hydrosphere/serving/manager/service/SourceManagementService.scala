package io.hydrosphere.serving.manager.service

import java.nio.file.Path
import akka.actor.{ActorRef, ActorSystem}
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Await
import scala.concurrent.duration._

trait SourceManagementService {
  def getSources: Seq[ModelSource]

  def getLocalPath(url: String): Path

  def createWatchers(context: ActorSystem): Seq[ActorRef]
}

class SourceManagementServiceImpl(sourceRepository: SourceConfigRepository) extends SourceManagementService with Logging {

  override def createWatchers(system: ActorSystem): Seq[ActorRef] =
    getSources.map { conf =>
      logger.info(s"SourceWatcher initialization: ${conf.configuration.name}")
      system.actorOf(SourceWatcher.props(conf), s"Watcher@${conf.configuration.name}")
    }

  override def getSources: Seq[ModelSource] =
    Await.result(sourceRepository.all(), 30.seconds).map(ModelSource.fromConfig)

  override def getLocalPath(url: String): Path = {
    val args = url.split(':')
    val source = args.head
    val path = args.last
    getSources
      .find(_.getSourcePrefix == source)
      .map(_.getAbsolutePath(path))
      .getOrElse(throw new IllegalArgumentException(s"Can't find ModelSource for prefix $source"))

  }
}
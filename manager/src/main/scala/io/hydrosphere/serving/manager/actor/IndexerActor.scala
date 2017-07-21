package io.hydrosphere.serving.manager.actor

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, ModelSourceConfiguration, S3ModelSourceConfiguration}
import io.hydrosphere.serving.manager.actor.modelsource.{LocalSourceWatcher, S3SourceWatcher}
import io.hydrosphere.serving.manager.service.ModelManagementService
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.service.modelsource.ModelSource

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Indexer {

  case object Index

}

class IndexerActor[S <: ModelSource, C <: ModelSourceConfiguration](
  val source: S,
  val config: C,
  val modelManagementService: ModelManagementService
) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.dispatcher

  private[this] val fsWatcher = context.actorOf(config match {
    case (c: LocalModelSourceConfiguration) =>
      LocalSourceWatcher.props(c, self)
    case (c: S3ModelSourceConfiguration) =>
      S3SourceWatcher.props(c, self)
    case _ =>
      throw new IllegalArgumentException(s"Unknown data config: $config")
  })

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, Indexer.Index.getClass)
    log.info(s"IndexerActor with: $source")
    self ! Indexer.Index
  }

  override def postStop(): Unit = fsWatcher ! PoisonPill

  override def receive: Receive = {
    case Indexer.Index =>
      log.info("Indexing...")
      val models = ModelFetcher.getModels(source)
      models
        .foreach(m => modelManagementService.updatedInModelSource(m) onComplete {
          case Success(_) => log.debug("Model updated: {}", m)
          case Failure(t) => log.error(t, "Can't update model")
        })
  }
}

object IndexerActor {
  def props[S <: ModelSource, C <: ModelSourceConfiguration](source: S, config: C, modelManagementService: ModelManagementService) =
    Props(classOf[IndexerActor[S, C]], source, config, modelManagementService)
}
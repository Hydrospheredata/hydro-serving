package io.hydrosphere.serving.manager.actor

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import io.hydrosphere.serving.manager.actor.IndexerActor.Index
import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, ModelSourceConfiguration, S3ModelSourceConfiguration}
import io.hydrosphere.serving.manager.actor.modelsource.{LocalSourceWatcher, S3SourceWatcher}
import io.hydrosphere.serving.manager.service.ModelManagementService
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.service.modelsource.{LocalModelSource, ModelSource, S3ModelSource}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class IndexerActor(
  val source: ModelSource,
  val modelManagementService: ModelManagementService
) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.dispatcher

  private[this] val fsWatcher = context.actorOf(source match {
    case (c: LocalModelSource) =>
      LocalSourceWatcher.props(c, self)
    case (c: S3ModelSource) =>
      S3SourceWatcher.props(c, self)
    case _ =>
      throw new IllegalArgumentException(s"Unknown data config: ${source.configuration}")
  })

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, Index.getClass)
    log.info(s"IndexerActor with: $source")
    self ! Index
  }

  override def postStop(): Unit = fsWatcher ! PoisonPill

  override def receive: Receive = {
    case Index =>
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
  case object Index

  def props(source: ModelSource, modelManagementService: ModelManagementService) =
    Props(classOf[IndexerActor], source, modelManagementService)
}
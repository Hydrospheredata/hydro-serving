package io.hydrosphere.serving.manager.service.actors

import java.time.{Instant, LocalDateTime, Duration => JDuration}

import akka.actor.Props
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.Model
import io.hydrosphere.serving.manager.repository.ModelRepository
import io.hydrosphere.serving.manager.service.actors.RepositoryIndexActor.{IgnoreModel, IndexFinished, IndexStart}
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.service.modelsource.events.FileEvent

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._

class RepositoryIndexActor(modelRepository: ModelRepository)
  extends SelfScheduledActor(0.seconds, 100.millis)(Timeout(30.seconds)) {

  import context._

  context.system.eventStream.subscribe(self, classOf[FileEvent])

  // model name -> last event
  private[this] val queues = TrieMap.empty[String, FileEvent]

  private[this] val blacklist = mutable.ListBuffer.empty[String]

  override def recieveNonTick: Receive = {
    case IgnoreModel(name) =>
      blacklist += name
    case e: FileEvent =>
      val modelName = e.filename.split("/").head
      if (!blacklist.contains(modelName)) {
        log.info(s"[${e.source.sourceDef.name}] Detected a modification of $modelName model ...")
        queues += modelName -> e
      }
  }

  override def onTick(): Unit = {
    val now = Instant.now()
    val upgradeable = queues.toList.filter {
      case (_, event) =>
        val diff = JDuration.between(event.timestamp, now)
        diff.getSeconds > 10 // TODO move to config
    }

    upgradeable.foreach {
      case (modelName, event) =>
        context.system.eventStream.publish(IndexStart(modelName, event.source.sourceDef.name))
        log.info(s"[${event.source.sourceDef.name}] Reindexing $modelName ...")
        queues -= modelName
        updateModel(modelName, event.source).foreach { maybeModel =>
          context.system.eventStream.publish(IndexFinished(modelName, event.source.sourceDef.name))
          log.info(s"${maybeModel.map(_.name)} is updated")
        }
    }
  }

  def updateModel(modelName: String, modelSource: ModelSource): Future[Option[Model]] = {
    if (modelSource.isExist(modelName)) {
      // model is updated
      val modelMetadata = ModelFetcher.getModel(modelSource, modelName)
      modelRepository.get(modelMetadata.modelName).flatMap {
        case Some(oldModel) =>
          val newModel = Model(
            id = oldModel.id,
            name = modelMetadata.modelName,
            source = s"${modelSource.sourceDef.name}:${modelMetadata.modelName}",
            modelType = modelMetadata.modelType,
            description = None,
            modelContract = modelMetadata.contract,
            created = oldModel.created,
            updated = LocalDateTime.now()
          )
          modelRepository.update(newModel).map(_ => Some(newModel))
        case None =>
          val newModel = Model(
            id = -1,
            name = modelMetadata.modelName,
            source = s"${modelSource.sourceDef.name}:${modelMetadata.modelName}",
            modelType = modelMetadata.modelType,
            description = None,
            modelContract = modelMetadata.contract,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
          )
          modelRepository.create(newModel).map(x => Some(x))
      }
    } else {
      // model is deleted
      modelRepository.get(modelName).map { opt =>
        opt.map { model =>
          modelRepository.delete(model.id)
          model
        }
      }
    }
  }

}

object RepositoryIndexActor {
  case class IgnoreModel(modelName: String)
  case class IndexStart(modelName: String, sourcePrefix: String)
  case class IndexFinished(modelName: String, sourcePrefix: String)

  def props(modelRepository: ModelRepository) = Props(new RepositoryIndexActor(modelRepository))
}

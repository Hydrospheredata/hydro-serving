package io.hydrosphere.serving.manager.service.actors

import java.time.{Instant, Duration => JDuration}

import akka.actor.Props
import akka.util.Timeout
import io.hydrosphere.serving.manager.service.ModelManagementService
import io.hydrosphere.serving.manager.service.actors.RepositoryIndexActor.{IndexFinished, IndexStart}
import io.hydrosphere.serving.manager.service.modelsource.events.FileEvent

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._

class RepositoryIndexActor(val modelManagementService: ModelManagementService)
  extends SelfScheduledActor(0.seconds, 100.millis)(Timeout(30.seconds)) {
  import context._
  context.system.eventStream.subscribe(self, classOf[FileEvent])

  // model name -> last event
  private[this] val queues = TrieMap.empty[String, FileEvent]

  override def recieveNonTick: Receive = {
    case e: FileEvent =>
      val modelName = e.filename.split("/").head
      log.debug(s"[${e.source.sourceDef.name}] Detected a modification of $modelName model ...")
      queues += modelName -> e
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
        log.debug(s"[${event.source.sourceDef.name}] Reindexing $modelName ...")
        queues -= modelName
        modelManagementService.updateModel(modelName, event.source).foreach { maybeModel =>
          context.system.eventStream.publish(IndexFinished(modelName, event.source.sourceDef.name))
          log.debug(s"$maybeModel is updated")
        }
    }
  }

}

object RepositoryIndexActor {
  case class IndexStart(modelName: String, sourcePrefix: String)
  case class IndexFinished(modelName: String, sourcePrefix: String)

  def props(modelManagementService: ModelManagementService) = Props(new RepositoryIndexActor(modelManagementService))
}

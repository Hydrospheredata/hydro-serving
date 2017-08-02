package io.hydrosphere.serving.manager.actor

import akka.actor.{Actor, ActorLogging, Props}
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher.{FileCreated, FileDeleted, FileEvent, FileModified}
import io.hydrosphere.serving.manager.service.ModelManagementService

class RepositoryActor(val modelManagementService: ModelManagementService) extends Actor with ActorLogging {
  import context._
  context.system.eventStream.subscribe(self, classOf[FileEvent])

  override def receive: Receive = {
    case FileCreated(source, fileName, hash, createdAt) =>
      modelManagementService.createFileForModel(source, fileName, hash, createdAt, createdAt).onFailure {
        case ex =>
          log.error(ex, "Cannot get model for associated file")
      }

    case FileModified(source, fileName, hash, updatedAt) =>
      modelManagementService.updateOrCreateModelFile(source, fileName, hash, updatedAt, updatedAt).onFailure {
        case ex =>
          log.error(ex, "Cannot get changed file")
      }

    case FileDeleted(source, fileName) =>
      modelManagementService.deleteModelFile(fileName)
  }
}

object RepositoryActor {
  def props(modelManagementService: ModelManagementService) = Props(classOf[RepositoryActor], modelManagementService)
}

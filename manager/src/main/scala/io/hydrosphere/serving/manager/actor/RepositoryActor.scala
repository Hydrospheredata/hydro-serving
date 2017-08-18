package io.hydrosphere.serving.manager.actor

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, Props}
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher._
import io.hydrosphere.serving.manager.service.ModelManagementService

class RepositoryActor(val modelManagementService: ModelManagementService) extends Actor with ActorLogging {
  import context._
  context.system.eventStream.subscribe(self, classOf[FileEvent])

  override def receive: Receive = {
    case FileCreated(source, fileName, hash, createdAt) =>
      println(s"[$source] File created: $fileName")
      modelManagementService.createFileForModel(source, fileName, hash, createdAt, createdAt).onFailure {
        case ex =>
          log.error(ex, "Cannot get model for associated file")
      }

    case FileModified(source, fileName, hash, updatedAt) =>
      println(s"[$source] File edited: $fileName")

      modelManagementService.updateOrCreateModelFile(source, fileName, hash, updatedAt, updatedAt).onFailure {
        case ex =>
          log.error(ex, "Cannot get changed file")
      }

    case FileDeleted(source, fileName) =>
      println(s"[$source] File deleted: $fileName")
      if (fileName.split('/').length == 1) {
        modelManagementService.deleteModel(fileName)
      } else {
        modelManagementService.deleteModelFile(fileName)
      }

    case FileDetected(source, filename, hash) =>
      modelManagementService.updateOrCreateModelFile(source, filename, hash, LocalDateTime.now(), LocalDateTime.now())
  }
}

object RepositoryActor {
  def props(modelManagementService: ModelManagementService) = Props(classOf[RepositoryActor], modelManagementService)
}

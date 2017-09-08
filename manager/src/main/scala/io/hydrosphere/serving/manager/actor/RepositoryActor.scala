package io.hydrosphere.serving.manager.actor

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher._
import io.hydrosphere.serving.manager.service.ModelManagementService

import scala.concurrent.Await
import scala.concurrent.duration._

class RepositoryActor(val modelManagementService: ModelManagementService) extends Actor with ActorLogging {
  import context._
  context.system.eventStream.subscribe(self, classOf[FileEvent])
  implicit private val timeout = Timeout(10.seconds)
  private val timer = context.system.scheduler.schedule(0.seconds, 100.millis, self, Tick)

  private[this] val queue = new ConcurrentLinkedQueue[FileEvent]()

  def processEvents(): Unit = {
    Option(queue.poll()).foreach{
      case FileCreated(source, fileName, hash, createdAt) =>
        println(s"[$source] File creation detected: $fileName")
        val f = modelManagementService.createFileForModel(source, fileName, hash, createdAt, createdAt)
        f.onFailure {
          case ex =>
            log.error(ex, "Cannot get model for associated file")
        }

      case FileModified(source, fileName, hash, updatedAt) =>
        println(s"[$source] File edition detected: $fileName")

        modelManagementService.updateOrCreateModelFile(source, fileName, hash, updatedAt, updatedAt).onFailure {
          case ex =>
            log.error(ex, "Cannot get changed file")
        }

      case FileDeleted(source, fileName) =>
        println(s"[$source] File deletion detected: $fileName")
        if (fileName.split('/').length == 1) {
          modelManagementService.deleteModel(fileName)
        } else {
          modelManagementService.deleteModelFile(fileName)
        }

      case FileDetected(source, filename, hash) =>
        println(s"[$source] File detected: $filename")
        Await.result(
          modelManagementService.updateOrCreateModelFile(source, filename, hash, LocalDateTime.now(), LocalDateTime.now()),
          5.minutes
        )
    }
  }

  override def receive: Receive = {
    case e: FileEvent =>
      queue.add(e)

    case Tick =>
      processEvents()
  }

  final override def postStop(): Unit = {
    timer.cancel()
  }
}

object RepositoryActor {
  case object Tick

  def props(modelManagementService: ModelManagementService) = Props(classOf[RepositoryActor], modelManagementService)
}

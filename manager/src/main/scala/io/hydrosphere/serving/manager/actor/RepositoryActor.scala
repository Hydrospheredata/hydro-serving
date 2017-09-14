package io.hydrosphere.serving.manager.actor

import java.time.{Instant, Duration => JDuration}

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher._
import io.hydrosphere.serving.manager.service.ModelManagementService

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._

class RepositoryActor(val modelManagementService: ModelManagementService) extends Actor with ActorLogging {
  import context._
  context.system.eventStream.subscribe(self, classOf[FileEvent])
  implicit private val timeout = Timeout(10.seconds)
  private val timer = context.system.scheduler.schedule(0.seconds, 100.millis, self, Tick)

  // model name -> last event
  private[this] val queues = TrieMap.empty[String, FileEvent]

  override def receive: Receive = {
    case e: FileEvent =>
      val modelName = e.filename.split("/").head
      println(s"[${e.source.getSourcePrefix}] Detected a modification of $modelName model ...")
      queues += modelName -> e

    case Tick =>
      val now = Instant.now()
      val upgradeable =  queues.toList.filter{
        case (_, event) =>
          val diff = JDuration.between(event.timestamp, now)
          diff.getSeconds > 10 // TODO move to config
      }

      upgradeable.foreach{
        case (modelname, event) =>
          println(s"[${event.source.getSourcePrefix}] Reindexing $modelname ...")
          queues -= modelname
          val r = modelManagementService.updateModel(modelname, event.source)
          r.foreach( x => println(s"$x is updated"))
      }
  }

  final override def postStop(): Unit = {
    timer.cancel()
  }
}

object RepositoryActor {
  case object Tick

  def props(modelManagementService: ModelManagementService) = Props(classOf[RepositoryActor], modelManagementService)
}

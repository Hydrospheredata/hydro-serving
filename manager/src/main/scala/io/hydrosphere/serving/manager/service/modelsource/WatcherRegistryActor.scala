package io.hydrosphere.serving.manager.service.modelsource

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.hydrosphere.serving.manager.service.modelsource.WatcherRegistryActor.{AddWatcher, ListWatchers}

import scala.collection.concurrent.TrieMap

class WatcherRegistryActor extends Actor with ActorLogging {
  private[this] val watchers = TrieMap.empty[String, ActorRef]

  override def receive = {
    case ListWatchers =>
      watchers.toMap
    case AddWatcher(source) =>
      log.info(s"Creating Watcher for ${source.sourceDef.prefix}")
      val origin = sender()
      val watcherRef = watchers.getOrElseUpdate(
        source.sourceDef.prefix,
        context.actorOf(SourceWatcherActor.props(source), s"Watcher@${source.sourceDef.prefix}")
      )
      origin ! watcherRef
  }
}

object WatcherRegistryActor {
  case object ListWatchers
  case class AddWatcher(source: ModelSource)

  def props = Props(classOf[WatcherRegistryActor])
}
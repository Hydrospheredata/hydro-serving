package io.hydrosphere.serving.repository.repository

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import io.hydrosphere.serving.repository.Messages
import io.hydrosphere.serving.repository.datasource.DataSource
import io.hydrosphere.serving.repository.ml.Runtime

/**
  * Created by bulat on 05.07.17.
  */

class IndexerActor[T <: DataSource](val source: T) extends Actor with ActorLogging {
  private[this] val fsWatcher = context.actorOf(source.watcherProps(self))

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, Messages.Indexer.Index.getClass)
    log.info(s"IndexerActor with: $source")
    self ! Messages.Indexer.Index
  }

  override def postStop(): Unit = fsWatcher ! PoisonPill

  override def receive: Receive = {
    case Messages.Indexer.Index =>
      log.info("Indexing...")
      val models = Runtime.getModels(source)
      val indexed = models.map { IndexEntry(_, source) }
      context.system.eventStream.publish(Messages.Indexer.IndexedModels(indexed))

    case Messages.Watcher.ChangeDetected =>
      self ! Messages.Indexer.Index
  }
}

object IndexerActor {
  def props[T <: DataSource](source: T) =
    Props(classOf[IndexerActor[T]], source)
}
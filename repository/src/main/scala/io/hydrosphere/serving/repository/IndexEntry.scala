package io.hydrosphere.serving.repository

import akka.actor.ActorRef
import io.hydrosphere.serving.repository.source.ModelSource

/**
  * Created by Bulat on 02.06.2017.
  */
case class IndexEntry(model: Model, watcher: ActorRef, source: ModelSource)

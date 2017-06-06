package io.prototypes.ml_repository

import akka.actor.ActorRef
import io.prototypes.ml_repository.source.ModelSource

/**
  * Created by Bulat on 02.06.2017.
  */
case class IndexEntry(model: Model, watcher: ActorRef, source: ModelSource)

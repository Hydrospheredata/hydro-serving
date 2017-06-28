package io.prototypes.ml_repository

import akka.actor.ActorRef
import io.prototypes.ml_repository.source.{LocalSource, ModelSource}
/**
  * Created by Bulat on 31.05.2017.
  */
object Messages {
  object RepositoryActor {
    case object GetIndex
    case class GetModelIndexEntry(name: String)
    case class GetModelFiles(name: String)
    case class RetrieveModelFiles(indexEntry: IndexEntry)
    case class GetModelDirectory(name: String)
  }

  object Watcher {
    case class Register(source: ModelSource)
    case object GetModels
    case object LookForChanges
    case class IndexedModels(models: Seq[IndexEntry])
    case class GetModelDirectory(indexEntry: IndexEntry)
  }
}

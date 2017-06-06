package io.prototypes.ml_repository.watcher

import java.io.{File => JFile}
import java.nio.file._
import java.nio.file.StandardWatchEventKinds._

import akka.actor.{ActorRef, Props}
import akka.util.Timeout

import collection.JavaConverters._
import scala.concurrent.duration._
import scala.collection.mutable
import io.prototypes.ml_repository.{IndexEntry, Messages}
import io.prototypes.ml_repository.runtime.RuntimeDispatcher
import io.prototypes.ml_repository.source.LocalSource
import io.prototypes.ml_repository.utils.FileUtils._


/**
  * Created by Bulat on 31.05.2017.
  */
class LocalSourceWatcher(val source: LocalSource) extends SourceWatcher {
  import context._
  implicit val timeout = Timeout(10.seconds)
  private val watcher = FileSystems.getDefault.newWatchService()
  private val keys = mutable.Map.empty[WatchKey, Path]
  private val subscribers = mutable.Buffer.empty[ActorRef]

  val sourceFile = new JFile(source.path.toString)

  override def preStart(): Unit = {
    context.system.scheduler.schedule(0.seconds, 500.millis, self, Messages.Watcher.LookForChanges)
    // TODO check if dir is really a directory
    val dirPath = Paths.get(sourceFile.getAbsolutePath).normalize()
    log.info(s"LocalWatchService with: $dirPath")

    keys += dirPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY) -> dirPath

    val subDirs = sourceFile.getSubDirectories

    subDirs.foreach { d =>
      val dPath = Paths.get(d.getCanonicalPath).normalize()
      keys += dPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY) -> dPath
    }
  }

  override def receive: Receive = {
    case Messages.Watcher.Subscribe(actorRef) =>
      log.info(s"New subscriber: $actorRef")
      subscribers += actorRef

    case Messages.Watcher.GetModels =>
      val models = RuntimeDispatcher.getModels(sourceFile.getSubDirectories)
      val indexed = models.map{m =>
        IndexEntry(m, self, source)
      }
      sender() ! Messages.Watcher.IndexedModels(indexed)

    case Messages.Watcher.LookForChanges =>
      for {
        (key, path) <- keys
        event <- key.pollEvents().asScala
      } {
        val kind = event.kind()
        if (kind == StandardWatchEventKinds.OVERFLOW) {
          log.info(s"Overflow: $event")
        } else {
          val name = event.context().asInstanceOf[Path]
          val child = path.resolve(name)
          log.info(s"LocalSource event: ${event.kind.name}, $child")
          subscribers.foreach(_ ! Messages.RepositoryActor.MakeIndex)
        }
      }

    case Messages.RepositoryActor.RetrieveModelFiles(index) =>
      val origin = sender()
      log.info(s"RetrieveModel($index)")
      val modelPath = Paths.get(sourceFile.toString, index.model.runtime, index.model.name)
      val modelFiles = modelPath.toFile.listFilesRecursively
      log.info(s"RetrieveModel result: $modelFiles")
      origin ! modelFiles.map(u => modelPath.toUri.relativize(u.toURI))

    case Messages.Watcher.GetModelDirectory(indexEntry) =>
      val origin = sender()
      log.info(s"GetModelDirectory($indexEntry)")
      val modelDirPath = Paths.get(sourceFile.toString, indexEntry.model.runtime, indexEntry.model.name)
      origin ! modelDirPath
  }
}

object LocalSourceWatcher {
  def props(localSource: LocalSource) = Props(classOf[LocalSourceWatcher], localSource)
}
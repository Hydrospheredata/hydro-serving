package io.hydrosphere.serving.manager.actor.modelsource

import akka.actor.{ActorRef, Props}
import awscala.sqs._
import io.hydrosphere.serving.manager.actor.IndexerActor.Index
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher.{FileEvent, FileModified}
import io.hydrosphere.serving.manager.service.modelsource.{ModelSource, S3ModelSource}


/**
  * Created by bulat on 04.07.17.
  */
class S3SourceWatcher(val source: S3ModelSource, val indexer: ActorRef) extends SourceWatcher {

  private[this] implicit val sqs = SQS.at(source.configuration.region)
  private[this] val queue = sqs.queue(source.configuration.queue).get

  override def onWatcherTick(): List[FileEvent] = {
    queue.messages() match {
      case msgs: List[Message] =>
        queue.removeAll(msgs)
        msgs.map { msg =>
          FileModified(msg.id)
        }
      case Nil =>
        Nil
    }
  }
}

object S3SourceWatcher{
  def props(source: S3ModelSource, indexer: ActorRef)=
    Props(classOf[S3SourceWatcher], source, indexer)
}
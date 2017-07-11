package io.hydrosphere.serving.repository.datasource.s3

import akka.actor.{ActorRef, Props}
import awscala.sqs._
import io.hydrosphere.serving.repository.Messages
import io.hydrosphere.serving.repository.datasource.SourceWatcher


/**
  * Created by bulat on 04.07.17.
  */
class S3SourceWatcher(val source: S3Source, val indexer: ActorRef) extends SourceWatcher {
  private[this] implicit val sqs = SQS.at(source.region)
  private[this] val queue = sqs.queue(source.queue).get

  override def onWatcherTick(): Unit = {
    val msgs = queue.messages()
    if (msgs.nonEmpty) {
      indexer ! Messages.Watcher.ChangeDetected
      queue.removeAll(msgs)
    }
  }
}

object S3SourceWatcher {
 def props(source: S3Source, indexer: ActorRef) = Props(classOf[S3SourceWatcher], source, indexer)
}
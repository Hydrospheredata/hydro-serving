package io.hydrosphere.serving.manager.actor.modelsource

import akka.actor.{ActorRef, Props}
import awscala.sqs._
import io.hydrosphere.serving.manager.S3ModelSourceConfiguration
import io.hydrosphere.serving.manager.actor.Indexer


/**
  * Created by bulat on 04.07.17.
  */
class S3SourceWatcher(val source: S3ModelSourceConfiguration, val indexer: ActorRef) extends SourceWatcher {
  private[this] implicit val sqs = SQS.at(source.region)
  private[this] val queue = sqs.queue(source.queue).get

  override def onWatcherTick(): Unit = {
    val msgs = queue.messages()
    if (msgs.nonEmpty) {
      indexer ! Indexer.Index
      queue.removeAll(msgs)
    }
  }
}

object S3SourceWatcher{
  def props(source: S3ModelSourceConfiguration, indexer: ActorRef)=
    Props(classOf[S3SourceWatcher], source, indexer)
}
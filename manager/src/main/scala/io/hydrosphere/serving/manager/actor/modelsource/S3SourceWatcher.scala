package io.hydrosphere.serving.manager.actor.modelsource

import akka.actor.{ActorRef, Props}
import awscala.sqs._
import io.hydrosphere.serving.manager.S3ModelSourceConfiguration
import io.hydrosphere.serving.manager.actor.Indexer
import io.hydrosphere.serving.manager.service.modelsource.S3ModelSource


/**
  * Created by bulat on 04.07.17.
  */
class S3SourceWatcher(val source: S3ModelSource, val indexer: ActorRef) extends SourceWatcher {
  private[this] implicit val sqs = SQS.at(source.configuration.region)
  private[this] val queue = sqs.queue(source.configuration.queue).get

  override def onWatcherTick(): Unit = {
    val msgs = queue.messages()
    if (msgs.nonEmpty) {
      indexer ! Indexer.Index

      queue.removeAll(msgs)
    }
  }
}

object S3SourceWatcher{
  def props(source: S3ModelSource, indexer: ActorRef)=
    Props(classOf[S3SourceWatcher], source, indexer)
}
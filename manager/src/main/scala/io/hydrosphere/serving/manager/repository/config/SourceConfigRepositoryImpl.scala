package io.hydrosphere.serving.manager.repository.config

import java.util.concurrent.ConcurrentLinkedQueue

import io.hydrosphere.serving.manager.ModelSourceConfiguration
import io.hydrosphere.serving.manager.repository.SourceConfigRepository

import scala.concurrent.Future
import scala.collection.JavaConversions._

class SourceConfigRepositoryImpl(sources: Seq[ModelSourceConfiguration]) extends SourceConfigRepository {
  import scala.concurrent.ExecutionContext.Implicits._

  val buffer = new ConcurrentLinkedQueue(sources)

  override def create(entity: ModelSourceConfiguration): Future[ModelSourceConfiguration] = {
    Future({
      buffer.add(entity)
      entity
    })
  }

  override def get(id: Long): Future[Option[ModelSourceConfiguration]] = ???

  override def delete(id: Long): Future[Int] = ???

  override def all(): Future[Seq[ModelSourceConfiguration]] = Future(buffer.toSeq)
}

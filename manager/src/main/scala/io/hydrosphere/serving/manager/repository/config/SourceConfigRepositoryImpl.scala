package io.hydrosphere.serving.manager.repository.config

import java.util.concurrent.ConcurrentLinkedQueue

import io.hydrosphere.serving.manager.{ManagerConfiguration, ModelSourceConfiguration}
import io.hydrosphere.serving.manager.repository.SourceConfigRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConversions._

class SourceConfigRepositoryImpl(config: ManagerConfiguration)(implicit ec: ExecutionContext) extends SourceConfigRepository {
  val buffer = new ConcurrentLinkedQueue(config.modelSources)

  override def create(entity: ModelSourceConfiguration): Future[ModelSourceConfiguration] = {
    Future.successful({
      buffer.add(entity)
      entity
    })
  }

  override def get(id: Long): Future[Option[ModelSourceConfiguration]] = ???

  override def delete(id: Long): Future[Int] = ???

  override def all(): Future[Seq[ModelSourceConfiguration]] = Future.successful(buffer.toList)
}

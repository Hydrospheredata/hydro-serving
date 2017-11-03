package io.hydrosphere.serving.manager.repository.config

import io.hydrosphere.serving.manager.{ManagerConfiguration, ModelSourceConfiguration}
import io.hydrosphere.serving.manager.repository.SourceConfigRepository

import scala.concurrent.{ExecutionContext, Future}

class SourceConfigRepositoryImpl(val config: ManagerConfiguration)(implicit ec: ExecutionContext) extends SourceConfigRepository {
  val sources = config.modelSources

  override def create(entity: ModelSourceConfiguration): Future[ModelSourceConfiguration] = ???

  override def get(id: Long): Future[Option[ModelSourceConfiguration]] = ???

  override def delete(id: Long): Future[Int] = ???

  override def all(): Future[Seq[ModelSourceConfiguration]] = Future.successful(sources)
}

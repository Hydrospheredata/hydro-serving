package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.model.db.PullRuntime
import io.hydrosphere.serving.manager.repository.RuntimePullRepository

import scala.concurrent.Future

class RuntimePullRepositoryImpl extends RuntimePullRepository {
  override def create(entity: PullRuntime): Future[PullRuntime] = ???

  override def get(id: Long): Future[Option[PullRuntime]] = ???

  override def delete(id: Long): Future[Int] = ???

  override def all(): Future[Seq[PullRuntime]] = ???

  override def getRunningPull(imageName: String, imageVersion: String): Future[Option[PullRuntime]] = ???

  override def update(entity: PullRuntime): Future[Int] = ???
}

package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Model

import scala.concurrent.Future

trait ModelRepository extends BaseRepository[Model, Long] {
  def fetchByModelType(types: Seq[ModelType]): Future[Seq[Model]]

  def get(name: String): Future[Option[Model]]

  def getMany(ids: Set[Long]): Future[Seq[Model]]

  def update(value: Model): Future[Int]
}

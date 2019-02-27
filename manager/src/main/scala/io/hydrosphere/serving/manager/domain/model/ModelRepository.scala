package io.hydrosphere.serving.manager.domain.model

import io.hydrosphere.serving.model.api.ModelType

import scala.concurrent.Future

trait ModelRepository[F[_]] {
  def create(entity: Model): F[Model]

  def get(id: Long): F[Option[Model]]

  def all(): F[Seq[Model]]

  def get(name: String): F[Option[Model]]

  def getMany(ids: Set[Long]): F[Seq[Model]]

  def update(value: Model): F[Int]

  def delete(id: Long): F[Int]
}

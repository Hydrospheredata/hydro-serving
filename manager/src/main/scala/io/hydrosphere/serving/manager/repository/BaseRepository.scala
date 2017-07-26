package io.hydrosphere.serving.manager.repository

import scala.concurrent.Future

/**
  *
  */
trait BaseRepository[T, ID] {
  def create(entity: T): Future[T]

  def get(id: ID): Future[Option[T]]

  def delete(id: ID): Future[Int]

  def all(): Future[Seq[T]]
}

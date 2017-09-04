package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.model.RuntimeType

import scala.concurrent.Future

/**
  *
  */
trait RuntimeTypeRepository extends BaseRepository[RuntimeType, Long] {

  def fetchByName(name: String): Future[Seq[RuntimeType]]

  def fetchByNameAndVersion(name: String, version: String): Future[Option[RuntimeType]]
}

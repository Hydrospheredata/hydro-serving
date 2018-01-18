package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.Runtime
import io.hydrosphere.serving.manager.model.api.ModelType

import scala.concurrent.Future

/**
  *
  */
trait RuntimeRepository extends BaseRepository[Runtime, Long] {
  def fetchByModelType(modelType: Seq[ModelType]): Future[Seq[Runtime]]

  def fetchByName(name: String): Future[Seq[Runtime]]

  def fetchByNameAndVersion(name: String, version: String): Future[Option[Runtime]]

  def fetchByTags(tags: Seq[String]): Future[Seq[Runtime]]
}
package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.Runtime

import scala.concurrent.Future

/**
  *
  */
trait RuntimeRepository extends BaseRepository[Runtime, Long] {

  def fetchByName(name: String): Future[Seq[Runtime]]

  def fetchByNameAndVersion(name: String, version: String): Future[Option[Runtime]]

  def fetchByTags(tags: Seq[String]): Future[Seq[Runtime]]
}
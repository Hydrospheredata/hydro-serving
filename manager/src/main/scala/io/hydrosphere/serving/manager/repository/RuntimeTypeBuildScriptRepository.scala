package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.RuntimeTypeBuildScript

import scala.concurrent.Future

/**
  *
  */
trait RuntimeTypeBuildScriptRepository {
  def create(entity: RuntimeTypeBuildScript): Future[RuntimeTypeBuildScript]

  def get(name: String, version: Option[String]): Future[Option[RuntimeTypeBuildScript]]

  def delete(name: String, version: Option[String]): Future[Int]

  def all(): Future[Seq[RuntimeTypeBuildScript]]
}

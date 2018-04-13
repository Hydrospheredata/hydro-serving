package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.db.ModelBuildScript

import scala.concurrent.Future

/**
  *
  */
trait ModelBuildScriptRepository {
  def create(entity: ModelBuildScript): Future[ModelBuildScript]

  def get(name: String): Future[Option[ModelBuildScript]]

  def delete(name: String): Future[Int]

  def all(): Future[Seq[ModelBuildScript]]
}

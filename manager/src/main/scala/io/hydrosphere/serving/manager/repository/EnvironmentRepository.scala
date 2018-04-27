package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.db.Environment

import scala.concurrent.Future


trait EnvironmentRepository  extends BaseRepository[Environment, Long] {
  def get(name: String): Future[Option[Environment]]
}

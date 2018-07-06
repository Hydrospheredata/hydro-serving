package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.db.PullRuntime

import scala.concurrent.Future

trait RuntimePullRepository extends BaseRepository[PullRuntime, Long]{
  def getRunningPull(imageName: String, imageVersion: String): Future[Option[PullRuntime]]
}

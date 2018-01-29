package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.{Application, ServiceKeyDescription}

import scala.concurrent.Future

/**
  *
  */
trait ApplicationRepository extends BaseRepository[Application, Long] {
  def update(value: Application): Future[Int]

  def getByName(name: String): Future[Option[Application]]

  def getLockForApplications(): Future[Any]

  def returnLockForApplications(lockInfo: Any): Future[Unit]

  def getKeysNotInApplication(keysSet: Set[ServiceKeyDescription], applicationId: Long):Future[Seq[Application]]
}

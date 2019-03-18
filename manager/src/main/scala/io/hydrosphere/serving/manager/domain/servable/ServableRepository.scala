package io.hydrosphere.serving.manager.domain.servable


trait ServableRepository[F[_]] {
  def create(entity: Servable): F[Servable]

  def get(id: Long): F[Option[Servable]]

  def delete(id: Long): F[Int]

  def all(): F[Seq[Servable]]

  def fetchByIds(seq: Seq[Long]): F[Seq[Servable]]

  def updateCloudDriveId(serviceId: Long, cloudDriveId: Option[String]): F[Int]
}

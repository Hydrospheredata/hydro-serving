package io.hydrosphere.serving.manager.domain.service


trait ServiceRepositoryAlgebra[F[_]] {
  def create(entity: Service): F[Service]

  def get(id: Long): F[Option[Service]]

  def delete(id: Long): F[Int]

  def all(): F[Seq[Service]]

  def fetchServices(services: Set[Long]): F[Seq[Service]]

  def fetchByIds(seq: Seq[Long]): F[Seq[Service]]

  def updateCloudDriveId(serviceId: Long, cloudDriveId: Option[String]): F[Int]

  def getByServiceName(serviceName: String): F[Option[Service]]
}

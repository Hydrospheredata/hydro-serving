package io.hydrosphere.serving.manager.domain.servable


trait ServableRepository[F[_]] {
  def update(entity: Servable): F[Servable]

  def get(id: Long): F[Option[Servable]]

  def delete(id: Long): F[Int]

  def all(): F[Seq[Servable]]

  def fetchServices(services: Set[Long]): F[Seq[Servable]]

  def fetchByIds(seq: Seq[Long]): F[Seq[Servable]]

  def getByServiceName(serviceName: String): F[Option[Servable]]
}

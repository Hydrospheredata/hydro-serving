package io.hydrosphere.serving.manager.domain.application

trait ApplicationRepositoryAlgebra[F[_]] {
  def create(entity: Application): F[Application]

  def get(id: Long): F[Option[Application]]

  def update(value: Application): F[Int]

  def delete(id: Long): F[Int]

  def getByName(name: String): F[Option[Application]]

  def all(): F[Seq[Application]]

  def getKeysNotInApplication(keysSet: Set[Long], applicationId: Long): F[Seq[Application]]

  def findVersionsUsage(versionIdx: Long): F[Seq[Application]]
}

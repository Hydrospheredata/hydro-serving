package io.hydrosphere.serving.manager.domain.host_selector

trait HostSelectorRepositoryAlgebra[F[_]] {
  def create(entity: HostSelector): F[HostSelector]

  def get(id: Long): F[Option[HostSelector]]

  def get(name: String): F[Option[HostSelector]]

  def all(): F[Seq[HostSelector]]

  def delete(id: Long): F[Int]
}
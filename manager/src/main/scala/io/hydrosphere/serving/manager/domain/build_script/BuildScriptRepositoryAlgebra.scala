package io.hydrosphere.serving.manager.domain.build_script

trait BuildScriptRepositoryAlgebra[F[_]] {
  def create(entity: ModelBuildScript): F[ModelBuildScript]

  def get(name: String): F[Option[ModelBuildScript]]

  def delete(name: String): F[Int]

  def all(): F[Seq[ModelBuildScript]]
}

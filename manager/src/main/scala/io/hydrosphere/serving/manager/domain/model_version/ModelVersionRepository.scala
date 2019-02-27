package io.hydrosphere.serving.manager.domain.model_version

trait ModelVersionRepository[F[_]] {
  def create(entity: ModelVersion): F[ModelVersion]

  def update(id: Long, entity: ModelVersion): F[Int]

  def get(id: Long): F[Option[ModelVersion]]

  def get(modelName: String, modelVersion: Long): F[Option[ModelVersion]]

  def get(idx: Seq[Long]): F[Seq[ModelVersion]]

  def delete(id: Long): F[Int]

  def all(): F[Seq[ModelVersion]]

  def listForModel(modelId: Long): F[Seq[ModelVersion]]

  def lastModelVersionByModel(modelId: Long, max: Int): F[Seq[ModelVersion]]

  def modelVersionsByModelVersionIds(modelVersionIds: Set[Long]): F[Seq[ModelVersion]]
}
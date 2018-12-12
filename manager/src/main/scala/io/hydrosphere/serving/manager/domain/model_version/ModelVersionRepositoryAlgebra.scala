package io.hydrosphere.serving.manager.domain.model_version

import scala.concurrent.Future

trait ModelVersionRepositoryAlgebra[F[_]] {
  def create(entity: ModelVersion): Future[ModelVersion]

  def update(id: Long, entity: ModelVersion): Future[ModelVersion]

  def get(id: Long): Future[Option[ModelVersion]]

  def get(modelName: String, modelVersion: Long): Future[Option[ModelVersion]]

  def get(idx: Set[Long]): Future[Set[ModelVersion]]

  def delete(id: Long): Future[Int]

  def all(): Future[Seq[ModelVersion]]

  def listForModel(modelId: Long): F[Seq[ModelVersion]]

  def lastModelVersionByModel(modelId: Long, max: Int): F[Seq[ModelVersion]]

  def modelVersionByModelAndVersion(modelId: Long, version: Long): F[Option[ModelVersion]]

  def lastModelVersionForModels(modelIds: Seq[Long]): F[Seq[ModelVersion]]

  def modelVersionsByModelVersionIds(modelVersionIds: Set[Long]): F[Seq[ModelVersion]]
}
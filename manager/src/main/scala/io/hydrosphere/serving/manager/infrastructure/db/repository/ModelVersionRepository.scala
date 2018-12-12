package io.hydrosphere.serving.manager.infrastructure.db.repository

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.db
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepositoryAlgebra}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.model.api.ModelType

import scala.concurrent.{ExecutionContext, Future}

class ModelVersionRepository(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelVersionRepositoryAlgebra[Future] {

  import ModelVersionRepository._
  import databaseService._
  import databaseService.driver.api._

  override def create(entity: ModelVersion): Future[ModelVersion] =
    db.run(
      Tables.ModelVersion returning Tables.ModelVersion += Tables.ModelVersionRow(
        modelVersionId = entity.id,
        modelId = entity.model.id,
        modelVersion = entity.modelVersion,
        modelContract = entity.modelContract.toProtoString,
        createdTimestamp = entity.created,
        imageName = entity.imageName,
        imageTag = entity.imageTag,
        imageSha256 = entity.imageSHA256,
        modelType = entity.modelType.toTag,
        hostSelector = entity.hostSelector.map(_.id),
        finishedTimestamp = entity.finished,
        runtimename = entity.runtimeName,
        runtimeversion = entity.runtimeVersion,
        status = entity.status,
      )
    ).map(mapFromDb)

  override def get(id: Long): Future[Option[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .filter(_.modelVersionId === id)
        .result.headOption
    ).map(mapFromDb)

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.ModelVersion
        .filter(_.modelVersionId === id)
        .delete
    )

  override def all(): Future[Seq[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .result
    ).map(mapFromDb)

  override def lastModelVersionByModel(modelId: Long, max: Int): Future[Seq[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .filter(_.modelId === modelId)
        .sortBy(_.modelVersion.desc)
        .take(max)
        .result
    ).map(mapFromDb)

  override def modelVersionsByModelVersionIds(modelVersionIds: Set[Long]): Future[Seq[ModelVersion]] = {
    val action = Tables.ModelVersion
      .filter {
        _.modelVersionId inSetBind modelVersionIds
      }
      .result

    db.run(action).map(mapFromDb)
  }

  override def lastModelVersionForModels(modelIds: Seq[Long]): Future[Seq[ModelVersion]] = {
    val latestBuilds = for {
      (id, versions) <- Tables.ModelVersion.groupBy(_.modelId)
    } yield {
      id -> versions.map(_.modelVersion).max
    }

    val action = Tables.ModelVersion
      .filter {
        _.modelId inSetBind modelIds
      }
      .join(latestBuilds)
      .on { case (version, (modelId, _)) => modelId === version.modelId }
      .filter { case (version, (_, latestVersion)) => version.modelVersion === latestVersion }
      .map(_._1)
      .result

    db.run(action).map(mapFromDb)
  }

  override def modelVersionByModelAndVersion(modelId: Long, version: Long): Future[Option[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .filter(r => r.modelId === modelId && r.modelVersion === version)
        .sortBy(_.modelVersion.desc)
        .distinctOn(_.modelId)
        .result.headOption
    ).map(mapFromDb)

  override def listForModel(modelId: Long): Future[Seq[ModelVersion]] = db.run {
    Tables.ModelVersion
      .filter(_.modelId === modelId)
      .sortBy(_.modelVersion)
      .result
  }.map(mapFromDb)

  override def get(modelName: String, modelVersion: Long): Future[Option[ModelVersion]] = db.run {
    Tables.ModelVersion
      .join(Tables.Model)
      .on((mv, m) => mv.modelId === m.modelId)
      .filter(mv => (mv._1.modelVersion === modelVersion) && (mv._2.name === modelName))
      .result.headOption
  }.map(mapFromDb)
}

object ModelVersionRepository {

  def mapFromDb(tuples: Seq[Tables.ModelVersion#TableElementType]): Seq[ModelVersion] =
    tuples.map(mapFromDb)

  def mapFromDb(tuples: Option[Tables.ModelVersion#TableElementType]): Option[ModelVersion] =
    tuples.map(mapFromDb)

  def mapFromDb(modelVersion: Tables.ModelVersion#TableElementType): ModelVersion = {
    ModelVersion(
      id = modelVersion.modelVersionId,
      imageName = modelVersion.imageName,
      imageTag = modelVersion.imageTag,
      imageSHA256 = modelVersion.imageSha256,
      modelId = modelVersion.modelId,
      modelVersion = modelVersion.modelVersion,
      modelType = ModelType.fromTag(modelVersion.modelType),
      modelContract = ModelContract.fromAscii(modelVersion.modelContract),
      created = modelVersion.createdTimestamp,
      finished = modelVersion.finishedTimestamp,
      runtime = modelVersion.runtime,
      hostSelectorId = modelVersion.hostSelector,
      status = modelVersion.status,
      buildscript = modelVersion.script
    )
  }

  def mapFromDb(x: Option[(db.Tables.ModelVersionRow, db.Tables.ModelRow)]): Option[ModelVersion] =
    x.map(mapFromDb)

}
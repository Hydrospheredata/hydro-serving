package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.controller.ManagerJsonSupport
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.{Model, ModelVersion}
import io.hydrosphere.serving.manager.repository.ModelVersionRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ModelVersionRepositoryImpl(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelVersionRepository with Logging with ManagerJsonSupport {

  import databaseService._
  import databaseService.driver.api._
  import ModelVersionRepositoryImpl._

  override def create(entity: ModelVersion): Future[ModelVersion] =
    db.run(
      Tables.ModelVersion returning Tables.ModelVersion += Tables.ModelVersionRow(
        modelVersionId = entity.id,
        modelName = entity.modelName,
        modelVersion = entity.modelVersion,
        source = entity.source,
        modelContract = entity.modelContract.toString,
        createdTimestamp = entity.created,
        imageName = entity.imageName,
        imageTag = entity.imageTag,
        imageMd5 = entity.imageMD5,
        modelId = entity.model.map(m=>m.id),
        modelType = entity.modelType.toTag
      )
    ).map(s => mapFromDb(s, entity.model))

  override def get(id: Long): Future[Option[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .filter(_.modelVersionId === id)
        .joinLeft(Tables.Model)
        .on({ case (m, rt) => m.modelId === rt.modelId })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.ModelVersion
        .filter(_.modelVersionId === id)
        .delete
    )

  override def all(): Future[Seq[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .joinLeft(Tables.Model)
        .on({ case (m, rt) => m.modelId === rt.modelId })
        .result
    ).map(s => mapFromDb(s))

  override def lastModelVersionByModel(modelId: Long, max: Int): Future[Seq[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .filter(_.modelId === modelId)
        .joinLeft(Tables.Model)
        .on({ case (m, rt) => m.modelId === rt.modelId })
        .sortBy(_._1.modelVersionId.desc)
        .take(max)
        .result
    ).map(s => mapFromDb(s))

  override def lastModelVersionForModels(modelIds: Seq[Long]): Future[Seq[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .filter(_.modelId inSetBind modelIds)
        .joinLeft(Tables.Model)
        .on({ case (m, rt) => m.modelId === rt.modelId })
        .sortBy(_._1.modelVersionId.desc)
        .distinctOn(_._1.modelId.get)
        .result
    ).map(s => mapFromDb(s))

  override def modelVersionByModelAndVersion(modelId: Long, version: Long): Future[Option[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .filter(r => r.modelId === modelId && r.modelVersion === version)
        .joinLeft(Tables.Model)
        .on({ case (m, rt) => m.modelId === rt.modelId })
        .sortBy(_._1.modelVersionId.desc)
        .distinctOn(_._1.modelId.get)
        .result.headOption
    ).map(s => mapFromDb(s))
}

object ModelVersionRepositoryImpl extends ManagerJsonSupport {

  def mapFromDb(option: Option[(Tables.ModelVersion#TableElementType, Option[Tables.Model#TableElementType])]): Option[ModelVersion] =
    option.map {
      case (modelVersion, model) =>
        mapFromDb(
          modelVersion,
          ModelRepositoryImpl.mapFromDb(model)
        )
    }

  def mapFromDb(tuples: Seq[(Tables.ModelVersion#TableElementType, Option[Tables.Model#TableElementType])]): Seq[ModelVersion] =
    tuples.map {
      case (modelVersion, model) =>
        mapFromDb(
          modelVersion,
          ModelRepositoryImpl.mapFromDb(model)
        )
    }

  def mapFromDb(modelVersion: Tables.ModelVersion#TableElementType, model: Option[Model]): ModelVersion = {
    ModelVersion(
      id = modelVersion.modelVersionId,
      imageName = modelVersion.imageName,
      imageTag = modelVersion.imageTag,
      imageMD5 = modelVersion.imageMd5,
      modelName = modelVersion.modelName,
      modelVersion = modelVersion.modelVersion,
      source = modelVersion.source,
      modelType = ModelType.fromTag(modelVersion.modelType),
      modelContract = ModelContract.fromAscii(modelVersion.modelContract),
      created = modelVersion.createdTimestamp,
      model = model
    )
  }
}
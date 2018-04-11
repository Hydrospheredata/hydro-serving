package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.repository.ModelVersionRepository
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
import io.hydrosphere.serving.manager.model.db.{Model, ModelVersion}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ModelVersionRepositoryImpl(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelVersionRepository with Logging {

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
        imageSha256 = entity.imageSHA256,
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
    ).map( mapFromDb)

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
    ).map(mapFromDb)

  override def lastModelVersionByModel(modelId: Long, max: Int): Future[Seq[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .filter(_.modelId === modelId)
        .sortBy(_.modelVersion.desc)
        .joinLeft(Tables.Model)
        .on({ case (m, rt) => m.modelId === rt.modelId })
        .take(max)
        .result
    ).map(mapFromDb)

  override def modelVersionsByModelVersionIds(modelVersionIds: Seq[Long]): Future[Seq[ModelVersion]] = {
    val action = Tables.ModelVersion
      .filter {
        _.modelVersionId inSetBind modelVersionIds
      }
      .result

    db.run(action).map(mv => mapFromDb(mv.map((_, None))))
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
      .joinLeft(Tables.Model)
      .on { case ((modelVersion, _), model) => modelVersion.modelId === model.modelId }
      .map { case ((version, _), model) => version -> model }
      .result

    db.run(action).map(mapFromDb)
  }


  override def modelVersionByModelAndVersion(modelId: Long, version: Long): Future[Option[ModelVersion]] =
    db.run(
      Tables.ModelVersion
        .filter(r => r.modelId === modelId && r.modelVersion === version)
        .sortBy(_.modelVersion.desc)
        .joinLeft(Tables.Model)
        .on({ case (m, rt) => m.modelId === rt.modelId })
        .distinctOn(_._1.modelId.get)
        .result.headOption
    ).map(mapFromDb)
}

object ModelVersionRepositoryImpl {

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
      imageSHA256 = modelVersion.imageSha256,
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
package io.hydrosphere.serving.manager.repository.db

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.controller.ManagerJsonSupport
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.Model
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.repository.ModelRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ModelRepositoryImpl(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelRepository with Logging with ManagerJsonSupport {

  import databaseService._
  import databaseService.driver.api._
  import ModelRepositoryImpl._

  override def updateLastUpdatedTime(source: String, timestamp: LocalDateTime): Future[Int] = {
    val query = for {
      models <- Tables.Model if models.source === source
    } yield models.updatedTimestamp

    db.run(query.update(timestamp))
  }

  override def create(entity: Model): Future[Model] =
    db.run(
      Tables.Model returning Tables.Model += Tables.ModelRow(
        modelId = entity.id,
        name = entity.name,
        source = entity.source,
        modelType = entity.modelType.toTag,
        modelContract = entity.modelContract.toString,
        description = entity.description,
        createdTimestamp = entity.created,
        updatedTimestamp = entity.updated)
    ).map(mapFromDb)


  override def get(id: Long): Future[Option[Model]] =
    db.run(
      Tables.Model
        .filter(_.modelId === id)
        .result.headOption
    ).map(mapFromDb)

  override def get(name: String): Future[Option[Model]] =
    db.run(
      Tables.Model
        .filter(_.name === name)
        .result.headOption
    ).map(mapFromDb)

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.Model
        .filter(_.modelId === id)
        .delete
    )

  override def all(): Future[Seq[Model]] =
    db.run(
      Tables.Model
        .result
    ).map(mapFromDb)

  override def fetchBySource(source: String): Future[Seq[Model]] =
    db.run(
      Tables.Model
        .filter(_.source === source)
        .result
    ).map(mapFromDb)

  override def update(value: Model): Future[Int] = {
    val query = for {
      models <- Tables.Model if models.modelId === value.id
    } yield (
      models.name,
      models.source,
      models.modelType,
      models.description,
      models.updatedTimestamp,
      models.modelContract
    )

    db.run(query.update(
      value.name,
      value.source,
      value.modelType.toTag,
      value.description,
      value.updated,
      value.modelContract.toString
    ))
  }

  override def updateLastUpdatedTime(modelId: Long, timestamp: LocalDateTime): Future[Int] = {
    val query = for {
      models <- Tables.Model if models.modelId === modelId
    } yield models.updatedTimestamp

    db.run(query.update(timestamp))
  }

}

object ModelRepositoryImpl extends ManagerJsonSupport {
  def mapFromDb(model: Option[Tables.Model#TableElementType]): Option[Model] =
    model.map(mapFromDb)

  def mapFromDb(models: Seq[Tables.Model#TableElementType]): Seq[Model] =
    models.map { model =>
      mapFromDb(model)
    }

  def mapFromDb(model: Tables.Model#TableElementType): Model =
    Model(
      id = model.modelId,
      name = model.name,
      source = model.source,
      modelType = ModelType.fromTag(model.modelType),
      description = model.description,
      modelContract = ModelContract.fromAscii(model.modelContract),
      created = model.createdTimestamp,
      updated = model.updatedTimestamp
    )
}

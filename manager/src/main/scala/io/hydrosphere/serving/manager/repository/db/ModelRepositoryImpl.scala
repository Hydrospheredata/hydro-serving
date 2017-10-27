package io.hydrosphere.serving.manager.repository.db

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.controller.ManagerJsonSupport
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.model.RuntimeType
import io.hydrosphere.serving.manager.model.Model
import io.hydrosphere.serving.manager.repository.ModelRepository
import io.hydrosphere.serving.model_api.ModelApi
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ModelRepositoryImpl(implicit executionContext: ExecutionContext, databaseService: DatabaseService)
  extends ModelRepository with Logging with ManagerJsonSupport {

  import spray.json._
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
        entity.id,
        entity.name,
        entity.source,
        entity.runtimeType match {
          case Some(r) => Some(r.id)
          case _ => None
        },
        entity.outputFields.toJson,
        entity.inputFields.toJson,
        entity.description,
        entity.created,
        entity.updated)
    ).map(s => mapFromDb(s, entity.runtimeType))


  override def get(id: Long): Future[Option[Model]] =
    db.run(
      Tables.Model
        .filter(_.modelId === id)
        .joinLeft(Tables.RuntimeType)
        .on({ case (m, rt) => m.runtimeTypeId === rt.runtimeTypeId })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def get(name: String): Future[Option[Model]] =
    db.run(
      Tables.Model
        .filter(_.name === name)
        .joinLeft(Tables.RuntimeType)
        .on({ case (m, rt) => m.runtimeTypeId === rt.runtimeTypeId })
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
      Tables.Model.joinLeft(Tables.RuntimeType)
        .on({ case (m, rt) => m.runtimeTypeId === rt.runtimeTypeId })
        .result
    ).map(s => mapFromDb(s))

  override def fetchBySource(source: String): Future[Seq[Model]] =
    db.run(
      Tables.Model
        .filter(_.source === source)
        .joinLeft(Tables.RuntimeType)
        .on({ case (m, rt) => m.runtimeTypeId === rt.runtimeTypeId })
        .result
    ).map(s => mapFromDb(s))

  override def update(value: Model): Future[Int] = {
    val query = for {
      models <- Tables.Model if models.modelId === value.id
    } yield (
      models.name,
      models.source,
      models.runtimeTypeId,
      models.description,
      models.updatedTimestamp,
      models.outputFields,
      models.inputFields
    )

    db.run(query.update(
      value.name,
      value.source,
      value.runtimeType match {
        case Some(r) => Some(r.id)
        case _ => None
      },
      value.description,
      value.updated,
      value.outputFields.toJson,
      value.inputFields.toJson
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
  import spray.json._

  def mapFromDb(model: Option[(Tables.Model#TableElementType, Option[Tables.RuntimeType#TableElementType])]): Option[Model] = model match {
    case Some(tuple) =>
      Some(mapFromDb(tuple._1, tuple._2.map(t => RuntimeTypeRepositoryImpl.mapFromDb(t))))
    case _ => None
  }

  def mapFromDb(tuples: Seq[(Tables.Model#TableElementType, Option[Tables.RuntimeType#TableElementType])]): Seq[Model] = {
    tuples.map(tuple =>
      mapFromDb(tuple._1, tuple._2.map(t => RuntimeTypeRepositoryImpl.mapFromDb(t))))
  }

  def mapFromDb(model: Tables.Model#TableElementType, runtimeType: Option[RuntimeType]): Model = {
    Model(
      id = model.modelId,
      name = model.name,
      source = model.source,
      runtimeType = runtimeType,
      description = model.description,
      outputFields = model.outputFields.convertTo[ModelApi],
      inputFields = model.inputFields.convertTo[ModelApi],
      created = model.createdTimestamp,
      updated = model.updatedTimestamp
    )
  }
}

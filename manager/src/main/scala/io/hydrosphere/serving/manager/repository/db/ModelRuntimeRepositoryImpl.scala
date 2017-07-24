package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.{ModelRuntime, RuntimeType}
import io.hydrosphere.serving.manager.repository.ModelRuntimeRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ModelRuntimeRepositoryImpl(databaseService: DatabaseService)(implicit executionContext: ExecutionContext)
  extends ModelRuntimeRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import ModelRuntimeRepositoryImpl._

  override def create(entity: ModelRuntime): Future[ModelRuntime] =
    db.run(
      Tables.ModelRuntime returning Tables.ModelRuntime += Tables.ModelRuntimeRow(
        -1,
        entity.runtimeType match {
          case Some(r) => Some(r.id)
          case _ => None
        },
        entity.modelName,
        entity.modelVersion,
        entity.source,
        entity.outputFields,
        entity.inputFields,
        entity.created,
        entity.imageName,
        entity.imageTag,
        entity.imageMD5Tag,
        entity.modelId)
    ).map(s => mapFromDb(s, entity.runtimeType))

  override def get(id: Long): Future[Option[ModelRuntime]] =
    db.run(
      Tables.ModelRuntime
        .filter(_.runtimeId === id)
        .joinLeft(Tables.RuntimeType)
        .on({ case (m, rt) => m.runtimeTypeId === rt.runtimeTypeId })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.ModelRuntime
        .filter(_.runtimeId === id)
        .delete
    )

  override def all(): Future[Seq[ModelRuntime]] =
    db.run(
      Tables.ModelRuntime.joinLeft(Tables.RuntimeType)
        .on({ case (m, rt) => m.runtimeTypeId === rt.runtimeTypeId })
        .result
    ).map(s => mapFromDb(s))
}

object ModelRuntimeRepositoryImpl {
  def mapFromDb(model: Option[(Tables.ModelRuntime#TableElementType, Option[Tables.RuntimeType#TableElementType])]): Option[ModelRuntime] = model match {
    case Some(tuple) =>
      Some(mapFromDb(tuple._1, tuple._2.map(t => RuntimeTypeRepositoryImpl.mapFromDb(t))))
    case _ => None
  }

  def mapFromDb(tuples: Seq[(Tables.ModelRuntime#TableElementType, Option[Tables.RuntimeType#TableElementType])]): Seq[ModelRuntime] = {
    tuples.map(tuple =>
      mapFromDb(tuple._1, tuple._2.map(t => RuntimeTypeRepositoryImpl.mapFromDb(t))))
  }

  def mapFromDb(model: Tables.ModelRuntime#TableElementType, runtimeType: Option[RuntimeType]): ModelRuntime = {
    ModelRuntime(
      id = model.runtimeId,
      imageName = model.imageName,
      imageTag = model.imageTag,
      imageMD5Tag = model.imageMd5Tag,
      modelName = model.modelname,
      modelVersion = model.modelversion,
      source = model.source,
      runtimeType = runtimeType,
      outputFields = model.outputFields,
      inputFields = model.inputFields,
      created = model.createdTimestamp,
      modelId = model.modelId
    )
  }
}
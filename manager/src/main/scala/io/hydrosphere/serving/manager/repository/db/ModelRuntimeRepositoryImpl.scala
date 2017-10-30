package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.controller.ManagerJsonSupport
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.model.{ModelRuntime, RuntimeType}
import io.hydrosphere.serving.manager.repository.ModelRuntimeRepository
import io.hydrosphere.serving.model_api.ModelApi
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ModelRuntimeRepositoryImpl(implicit executionContext: ExecutionContext, databaseService: DatabaseService)
  extends ModelRuntimeRepository with Logging with ManagerJsonSupport {

  import spray.json._
  import databaseService._
  import databaseService.driver.api._
  import ModelRuntimeRepositoryImpl._

  override def create(entity: ModelRuntime): Future[ModelRuntime] =
    db.run(
      Tables.ModelRuntime returning Tables.ModelRuntime += Tables.ModelRuntimeRow(
        entity.id,
        entity.runtimeType match {
          case Some(r) => Some(r.id)
          case _ => None
        },
        entity.modelName,
        entity.modelVersion,
        entity.source,
        entity.outputFields.toJson,
        entity.inputFields.toJson,
        entity.created,
        entity.imageName,
        entity.imageTag,
        entity.imageMD5Tag,
        entity.modelId,
        entity.tags,
        entity.configParams.map { case (k, v) => s"$k=$v" }.toList
      )
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

  override def lastModelRuntimeByModel(modelId: Long, max: Int): Future[Seq[ModelRuntime]] =
    db.run(
      Tables.ModelRuntime
        .filter(_.modelId === modelId)
        .joinLeft(Tables.RuntimeType)
        .on({ case (m, rt) => m.runtimeTypeId === rt.runtimeTypeId })
        .sortBy(_._1.runtimeId.desc)
        .take(max)
        .result
    ).map(s => mapFromDb(s))

  override def lastModelRuntimeForModels(modelIds: Seq[Long]): Future[Seq[ModelRuntime]] =
    db.run(
      Tables.ModelRuntime
        .filter(_.modelId inSetBind modelIds)
        .joinLeft(Tables.RuntimeType)
        .on({ case (m, rt) => m.runtimeTypeId === rt.runtimeTypeId })
        .sortBy(_._1.runtimeId.desc)
        .distinctOn(_._1.modelId.get)
        .result
    ).map(s => mapFromDb(s))

  override def modelRuntimeByModelAndVersion(modelId: Long, version: String): Future[Option[ModelRuntime]] =
    db.run(
      Tables.ModelRuntime
        .filter(r => r.modelId === modelId && r.modelversion === version)
        .joinLeft(Tables.RuntimeType)
        .on({ case (m, rt) => m.runtimeTypeId === rt.runtimeTypeId })
        .sortBy(_._1.runtimeId.desc)
        .distinctOn(_._1.modelId.get)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def fetchByTags(tags: Seq[String]): Future[Seq[ModelRuntime]] =
    db.run(
      Tables.ModelRuntime
        .filter(p => p.tags @> tags.toList)
        .joinLeft(Tables.RuntimeType)
        .on({ case (m, rt) => m.runtimeTypeId === rt.runtimeTypeId })
        .result
    ).map(s => mapFromDb(s))
}

object ModelRuntimeRepositoryImpl extends ManagerJsonSupport {
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
      outputFields = model.outputFields.convertTo[ModelApi],
      inputFields = model.inputFields.convertTo[ModelApi],
      created = model.createdTimestamp,
      modelId = model.modelId,
      tags = model.tags,
      configParams = model.configParams.map(s => {
        val arr = s.split('=')
        arr.head -> arr.drop(1).mkString("=")
      }).toMap
    )
  }
}
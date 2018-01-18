package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.Runtime
import io.hydrosphere.serving.manager.repository.RuntimeRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class RuntimeRepositoryImpl(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends RuntimeRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import RuntimeRepositoryImpl.mapFromDb

  override def fetchByName(name: String): Future[Seq[Runtime]] =
    db.run(
      Tables.Runtime
        .filter(_.name === name)
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))

  override def fetchByNameAndVersion(name: String, version: String): Future[Option[Runtime]] =
    db.run(
      Tables.Runtime
        .filter(p => p.name === name && p.version === version)
        .result.headOption
    ).map(r => mapFromDb(r))

  override def create(entity: Runtime): Future[Runtime] =
    db.run(
      Tables.Runtime returning Tables.Runtime += Tables.RuntimeRow(
        entity.id,
        entity.name,
        entity.version,
        entity.tags,
        entity.configParams.map { case (k, v) => s"$k=$v" }.toList,
        entity.suitableModelType.map(p=>p.toTag)
      )
    ).map(s => mapFromDb(s))

  override def get(id: Long): Future[Option[Runtime]] =
    db.run(
      Tables.Runtime
        .filter(_.runtimeId === id)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.Runtime.filter(_.runtimeId === id)
        .delete
    )

  override def all(): Future[Seq[Runtime]] =
    db.run(
      Tables.Runtime
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))

  override def fetchByTags(tags: Seq[String]): Future[Seq[Runtime]] =
    db.run(
      Tables.Runtime
        .filter(p => p.tags @> tags.toList)
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))

  override def fetchByModelType(modelType: Seq[ModelType]): Future[Seq[Runtime]] =
    db.run(
      Tables.Runtime
        .filter(p => p.tags @> modelType.map(p=>p.toTag).toList)
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object RuntimeRepositoryImpl {
  def mapFromDb(dbType: Option[Tables.Runtime#TableElementType]): Option[Runtime] =
    dbType.map(r=>mapFromDb(r))

  def mapFromDb(dbType: Tables.Runtime#TableElementType): Runtime = {
    Runtime(
      id = dbType.runtimeId,
      name = dbType.name,
      version = dbType.version,
      suitableModelType = dbType.suitableModelTypes.map(p => ModelType.fromTag(p)),
      tags = dbType.tags,
      configParams = dbType.configParams.map { s =>
        val arr = s.split('=')
        arr.head -> arr.drop(1).mkString("=")
      }.toMap
    )
  }
}
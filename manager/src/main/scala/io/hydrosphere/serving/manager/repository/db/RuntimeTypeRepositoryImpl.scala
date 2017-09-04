package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.model.RuntimeType
import io.hydrosphere.serving.manager.repository.RuntimeTypeRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class RuntimeTypeRepositoryImpl(databaseService: DatabaseService)(implicit executionContext: ExecutionContext)
  extends RuntimeTypeRepository with Logging{

  import databaseService._
  import databaseService.driver.api._
  import RuntimeTypeRepositoryImpl.mapFromDb

  override def fetchByName(name: String): Future[Seq[RuntimeType]] =
    db.run(
      Tables.RuntimeType
        .filter(_.name === name)
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))

  override def fetchByNameAndVersion(name: String, version: String): Future[Option[RuntimeType]] =
    db.run(
      Tables.RuntimeType
        .filter(p => p.name === name && p.version === version)
        .result.headOption
    ).map(r => mapFromDb(r))

  override def create(entity: RuntimeType): Future[RuntimeType] =
    db.run(
      Tables.RuntimeType returning Tables.RuntimeType += Tables.RuntimeTypeRow(entity.id, entity.name, entity.version, entity.tags)
    ).map(s => mapFromDb(s))

  override def get(id: Long): Future[Option[RuntimeType]] =
    db.run(
      Tables.RuntimeType
        .filter(_.runtimeTypeId === id)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.RuntimeType.filter(_.runtimeTypeId === id)
        .delete
    )

  override def all(): Future[Seq[RuntimeType]] =
    db.run(
      Tables.RuntimeType
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object RuntimeTypeRepositoryImpl {
  def mapFromDb(dbType: Option[Tables.RuntimeType#TableElementType]): Option[RuntimeType] = dbType match {
    case Some(r: Tables.RuntimeType#TableElementType) =>
      Some(mapFromDb(r))
    case _ => None
  }

  def mapFromDb(dbType: Tables.RuntimeType#TableElementType): RuntimeType = {
    RuntimeType(
      id = dbType.runtimeTypeId,
      name = dbType.name,
      version = dbType.version,
      tags = dbType.tags
    )
  }
}
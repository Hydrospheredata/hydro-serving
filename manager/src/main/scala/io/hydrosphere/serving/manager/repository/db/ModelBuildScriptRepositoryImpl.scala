package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.ModelBuildScript
import io.hydrosphere.serving.manager.repository.ModelBuildScriptRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ModelBuildScriptRepositoryImpl(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelBuildScriptRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import ModelBuildScriptRepositoryImpl._

  override def create(entity: ModelBuildScript): Future[ModelBuildScript] =
    db.run(
      Tables.ModelBuildScript returning Tables.ModelBuildScript += Tables.ModelBuildScriptRow(
        entity.name,
        mapVersion(entity.version),
        entity.script
      )
    ).map(s => mapFromDb(s))

  override def get(name: String, version: Option[String]): Future[Option[ModelBuildScript]] =
    db.run(
      Tables.ModelBuildScript
        .filter(p => p.name === name && p.version === version)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(name: String, version: Option[String]): Future[Int] =
    db.run(
      Tables.ModelBuildScript
        .filter(p => p.name === name && p.version === version)
        .delete
    )

  override def all(): Future[Seq[ModelBuildScript]] =
    db.run(
      Tables.ModelBuildScript
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object ModelBuildScriptRepositoryImpl {

  private def mapVersion(v: Option[String]): String = v match {
    case None => ""
    case Some(x) => x
  }

  private def mapVersion(v: String): Option[String] = v match {
    case "" => None
    case x => Some(x)
  }

  def mapFromDb(dbType: Option[Tables.ModelBuildScript#TableElementType]): Option[ModelBuildScript] =
    dbType.map(r => mapFromDb(r))

  def mapFromDb(dbType: Tables.ModelBuildScript#TableElementType): ModelBuildScript = {
    ModelBuildScript(
      name = dbType.name,
      script = dbType.script,
      version = mapVersion(dbType.version)
    )
  }
}

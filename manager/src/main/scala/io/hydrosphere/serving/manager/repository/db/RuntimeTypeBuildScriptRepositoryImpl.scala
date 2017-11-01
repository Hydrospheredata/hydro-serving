package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.RuntimeTypeBuildScript
import io.hydrosphere.serving.manager.repository.RuntimeTypeBuildScriptRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class RuntimeTypeBuildScriptRepositoryImpl(implicit executionContext: ExecutionContext, databaseService: DatabaseService)
  extends RuntimeTypeBuildScriptRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import RuntimeTypeBuildScriptRepositoryImpl._

  override def create(entity: RuntimeTypeBuildScript): Future[RuntimeTypeBuildScript] =
    db.run(
      Tables.RuntimeTypeBuildScript returning Tables.RuntimeTypeBuildScript += Tables.RuntimeTypeBuildScriptRow(
        entity.name,
        mapVersion(entity.version),
        entity.script
      )
    ).map(s => mapFromDb(s))

  override def get(name: String, version: Option[String]): Future[Option[RuntimeTypeBuildScript]] =
    db.run(
      Tables.RuntimeTypeBuildScript
        .filter(p => p.name === name && p.version === version)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(name: String, version: Option[String]): Future[Int] =
    db.run(
      Tables.RuntimeTypeBuildScript
        .filter(p => p.name === name && p.version === version)
        .delete
    )

  override def all(): Future[Seq[RuntimeTypeBuildScript]] =
    db.run(
      Tables.RuntimeTypeBuildScript
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object RuntimeTypeBuildScriptRepositoryImpl {

  private def mapVersion(v: Option[String]): String = v match {
    case None => ""
    case Some(x) => x
  }

  private def mapVersion(v: String): Option[String] = v match {
    case "" => None
    case x => Some(x)
  }

  def mapFromDb(dbType: Option[Tables.RuntimeTypeBuildScript#TableElementType]): Option[RuntimeTypeBuildScript] = dbType match {
    case Some(r: Tables.RuntimeTypeBuildScript#TableElementType) =>
      Some(mapFromDb(r))
    case _ => None
  }

  def mapFromDb(dbType: Tables.RuntimeTypeBuildScript#TableElementType): RuntimeTypeBuildScript = {
    RuntimeTypeBuildScript(name = dbType.name, script = dbType.script, version = mapVersion(dbType.version))
  }
}

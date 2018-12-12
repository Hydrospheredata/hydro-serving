package io.hydrosphere.serving.manager.infrastructure.db.repository

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.build_script.{BuildScriptRepositoryAlgebra, ModelBuildScript}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class BuildScriptRepository(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends BuildScriptRepositoryAlgebra[Future] with Logging {

  import BuildScriptRepository._
  import databaseService._
  import databaseService.driver.api._

  override def create(entity: ModelBuildScript): Future[ModelBuildScript] =
    db.run(
      Tables.ModelBuildScript returning Tables.ModelBuildScript += Tables.ModelBuildScriptRow(
        entity.name,
        entity.script
      )
    ).map(s => mapFromDb(s))

  override def get(name: String): Future[Option[ModelBuildScript]] =
    db.run(
      Tables.ModelBuildScript
        .filter(p => p.name === name)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(name: String): Future[Int] =
    db.run(
      Tables.ModelBuildScript
        .filter(p => p.name === name)
        .delete
    )

  override def all(): Future[Seq[ModelBuildScript]] =
    db.run(
      Tables.ModelBuildScript
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object BuildScriptRepository {

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
      script = dbType.script
    )
  }
}

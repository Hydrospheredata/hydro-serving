package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.Environment
import io.hydrosphere.serving.manager.repository.EnvironmentRepository
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.manager.model.CommonJsonSupport._
import scala.concurrent.{ExecutionContext, Future}

class EnvironmentRepositoryImpl(implicit executionContext: ExecutionContext, databaseService: DatabaseService)
  extends EnvironmentRepository with Logging {

  import spray.json._
  import databaseService._
  import databaseService.driver.api._
  import EnvironmentRepositoryImpl._

  override def create(entity: Environment): Future[Environment] =
    db.run(
      Tables.Environment returning Tables.Environment += Tables.EnvironmentRow(
        entity.id,
        entity.name,
        entity.placeholders.map(p => p.toJson.toString()).toList
      )
    ).map(s => mapFromDb(s))

  override def get(id: Long): Future[Option[Environment]] =
    db.run(
      Tables.Environment
        .filter(_.environmentId === id)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.Environment.filter(_.environmentId === id)
        .delete
    )

  override def all(): Future[Seq[Environment]] =
    db.run(
      Tables.Environment
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object EnvironmentRepositoryImpl {

  import spray.json._

  def mapFromDb(dbType: Option[Tables.Environment#TableElementType]): Option[Environment] =
    dbType.map(r => mapFromDb(r))

  def mapFromDb(dbType: Tables.Environment#TableElementType): Environment = {
    Environment(
      id = dbType.environmentId,
      name = dbType.name,
      placeholders = dbType.placeholders.map(p => p.parseJson.convertTo[Any])
    )
  }
}

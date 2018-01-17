package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.controller.ManagerJsonSupport
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.{ModelSourceConfigAux, SourceParams}
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class SourceConfigRepositoryImpl(implicit ec: ExecutionContext, databaseService: DatabaseService)
  extends SourceConfigRepository with Logging with ManagerJsonSupport {

  import spray.json._
  import databaseService._
  import databaseService.driver.api._
  import SourceConfigRepositoryImpl._

  override def create(entity: ModelSourceConfigAux): Future[ModelSourceConfigAux] = db.run(
    Tables.ModelSource returning Tables.ModelSource += Tables.ModelSourceRow(
      entity.id,
      entity.name,
      entity.params.toJson.toString
    )
  ).map(mapFromDb)

  override def get(id: Long):Future[Option[ModelSourceConfigAux]] = db.run(
    Tables.ModelSource.filter(_.sourceId === id).result.headOption
  ).map(mapFromDb)

  override def delete(id: Long): Future[Int] = db.run(
    Tables.ModelSource.filter(_.sourceId === id).delete
  )

  override def all(): Future[Seq[ModelSourceConfigAux]] = {
    db.run(
      Tables.ModelSource.result
    ).map(mapFromDb)
  }
}

object SourceConfigRepositoryImpl extends ManagerJsonSupport {
  import spray.json._

  def mapFromDb(dbType: Option[Tables.ModelSource#TableElementType]): Option[ModelSourceConfigAux] =
    dbType.map(r => mapFromDb(r))

  def mapFromDb(dbType: Tables.ModelSource#TableElementType): ModelSourceConfigAux = {
    ModelSourceConfigAux(
      id = dbType.sourceId,
      name = dbType.name,
      params = dbType.params.parseJson.convertTo[SourceParams]
    )
  }

  def mapFromDb(dbType: Seq[Tables.ModelSource#TableElementType]): Seq[ModelSourceConfigAux] =
    dbType.map(mapFromDb)

}


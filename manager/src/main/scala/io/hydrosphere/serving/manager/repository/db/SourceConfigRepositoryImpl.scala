package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.SourceParams
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class SourceConfigRepositoryImpl(implicit ec: ExecutionContext, databaseService: DatabaseService)
  extends SourceConfigRepository with Logging {

  import spray.json._
  import databaseService._
  import databaseService.driver.api._
  import SourceConfigRepositoryImpl._

  override def create(entity: ModelSourceConfig): Future[ModelSourceConfig] = db.run(
    Tables.ModelSource returning Tables.ModelSource += Tables.ModelSourceRow(
      entity.id,
      entity.name,
      entity.params.toJson.toString
    )
  ).map(mapFromDb)

  override def get(id: Long): Future[Option[ModelSourceConfig]] = db.run(
    Tables.ModelSource.filter(_.sourceId === id).result.headOption
  ).map(mapFromDb)

  override def delete(id: Long): Future[Int] = db.run(
    Tables.ModelSource.filter(_.sourceId === id).delete
  )

  override def all(): Future[Seq[ModelSourceConfig]] = {
    db.run(
      Tables.ModelSource.result
    ).map(mapFromDb)
  }

  override def getByName(name: String): Future[Option[ModelSourceConfig]] = db.run {
    Tables.ModelSource.filter(_.name === name).result.headOption
  }.map(mapFromDb)
}

object SourceConfigRepositoryImpl {
  import spray.json._

  def mapFromDb(dbType: Option[Tables.ModelSource#TableElementType]): Option[ModelSourceConfig] =
    dbType.map(r => mapFromDb(r))

  def mapFromDb(dbType: Tables.ModelSource#TableElementType): ModelSourceConfig = {
    ModelSourceConfig(
      id = dbType.sourceId,
      name = dbType.name,
      params = dbType.params.parseJson.convertTo[SourceParams]
    )
  }

  def mapFromDb(dbType: Seq[Tables.ModelSource#TableElementType]): Seq[ModelSourceConfig] =
    dbType.map(mapFromDb)

}


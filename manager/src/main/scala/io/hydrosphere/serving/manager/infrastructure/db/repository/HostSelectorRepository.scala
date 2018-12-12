package io.hydrosphere.serving.manager.infrastructure.db.repository

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.host_selector.{HostSelector, HostSelectorRepositoryAlgebra}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class HostSelectorRepository(implicit executionContext: ExecutionContext, databaseService: DatabaseService)
  extends HostSelectorRepositoryAlgebra[Future] with Logging {

  import HostSelectorRepository._
  import databaseService._
  import databaseService.driver.api._

  override def create(entity: HostSelector): Future[HostSelector] =
    db.run(
      Tables.HostSelector returning Tables.HostSelector += Tables.HostSelectorRow(
        entity.id,
        entity.name,
        entity.placeholder
      )
    ).map(mapFromDb)

  override def get(id: Long): Future[Option[HostSelector]] =
    db.run(
      Tables.HostSelector
        .filter(_.hostSelectorId === id)
        .result.headOption
    ).map(mapFromDb)

  override def get(name: String): Future[Option[HostSelector]] = db.run(
    Tables.HostSelector
      .filter(_.name === name)
      .result.headOption
  ).map(mapFromDb)

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.HostSelector.filter(_.hostSelectorId === id)
        .delete
    )

  override def all(): Future[Seq[HostSelector]] =
    db.run(
      Tables.HostSelector
        .result
    ).map(s => s.map(mapFromDb))
}

object HostSelectorRepository {

  def mapFromDb(dbType: Option[Tables.HostSelector#TableElementType]): Option[HostSelector] =
    dbType.map(r => mapFromDb(r))

  def mapFromDb(dbType: Tables.HostSelector#TableElementType): HostSelector = {
    HostSelector(
      id = dbType.hostSelectorId,
      name = dbType.name,
      placeholder = dbType.placeholders
    )
  }
}

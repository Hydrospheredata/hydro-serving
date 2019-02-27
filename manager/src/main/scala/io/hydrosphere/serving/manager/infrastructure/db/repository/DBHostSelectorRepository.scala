package io.hydrosphere.serving.manager.infrastructure.db.repository

import cats.effect.Async
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.host_selector.{HostSelector, HostSelectorRepository}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

class DBHostSelectorRepository[F[_]: Async](implicit executionContext: ExecutionContext, databaseService: DatabaseService)
  extends HostSelectorRepository[F] with Logging {

  import DBHostSelectorRepository._
  import databaseService._
  import databaseService.driver.api._

  override def create(entity: HostSelector) = AsyncUtil.futureAsync {
    db.run(
      Tables.HostSelector returning Tables.HostSelector += Tables.HostSelectorRow(
        entity.id,
        entity.name,
        entity.placeholder
      )
    ).map(mapFromDb)
  }

  override def get(id: Long) = AsyncUtil.futureAsync {
    db.run(
      Tables.HostSelector
        .filter(_.hostSelectorId === id)
        .result.headOption
    ).map(mapFromDb)
  }

  override def get(name: String) = AsyncUtil.futureAsync {
    db.run(
      Tables.HostSelector
        .filter(_.name === name)
        .result.headOption
    ).map(mapFromDb)
  }

  override def delete(id: Long) = AsyncUtil.futureAsync {
    db.run(
      Tables.HostSelector.filter(_.hostSelectorId === id)
        .delete
    )
  }

  override def all() = AsyncUtil.futureAsync {
    db.run(
      Tables.HostSelector
        .result
    ).map(s => s.map(mapFromDb))
  }
}

object DBHostSelectorRepository {

  def mapFromDb(dbType: Option[Tables.HostSelector#TableElementType]): Option[HostSelector] =
    dbType.map(r => mapFromDb(r))

  def mapFromDb(dbType: Tables.HostSelector#TableElementType): HostSelector = {
    HostSelector(
      id = dbType.hostSelectorId,
      name = dbType.name,
      placeholder = dbType.placeholder
    )
  }
}

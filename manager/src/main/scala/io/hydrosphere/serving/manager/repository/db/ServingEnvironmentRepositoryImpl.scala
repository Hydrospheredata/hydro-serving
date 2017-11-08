package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.controller.ManagerJsonSupport
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.ServingEnvironment
import io.hydrosphere.serving.manager.repository.ServingEnvironmentRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ServingEnvironmentRepositoryImpl(implicit executionContext: ExecutionContext, databaseService: DatabaseService)
  extends ServingEnvironmentRepository with Logging with ManagerJsonSupport {

  import spray.json._
  import databaseService._
  import databaseService.driver.api._
  import ServingEnvironmentRepositoryImpl._

  override def create(entity: ServingEnvironment): Future[ServingEnvironment] =
    db.run(
      Tables.ServingEnvironment returning Tables.ServingEnvironment += Tables.ServingEnvironmentRow(
        entity.id,
        entity.name,
        entity.placeholders.map(p => p.toJson.toString()).toList
      )
    ).map(s => mapFromDb(s))

  override def get(id: Long): Future[Option[ServingEnvironment]] =
    db.run(
      Tables.ServingEnvironment
        .filter(_.environmentId === id)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.ServingEnvironment.filter(_.environmentId === id)
        .delete
    )

  override def all(): Future[Seq[ServingEnvironment]] =
    db.run(
      Tables.ServingEnvironment
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object ServingEnvironmentRepositoryImpl extends ManagerJsonSupport {

  import spray.json._

  def mapFromDb(dbType: Option[Tables.ServingEnvironment#TableElementType]): Option[ServingEnvironment] = dbType match {
    case Some(r: Tables.ServingEnvironment#TableElementType) =>
      Some(mapFromDb(r))
    case _ => None
  }

  def mapFromDb(dbType: Tables.ServingEnvironment#TableElementType): ServingEnvironment = {
    ServingEnvironment(
      id = dbType.environmentId,
      name = dbType.name,
      placeholders = dbType.placeholders.map(p => p.parseJson.convertTo[Any])
    )
  }
}

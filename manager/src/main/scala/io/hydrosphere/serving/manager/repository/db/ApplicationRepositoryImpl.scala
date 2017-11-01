package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.model.{Application, ApplicationExecutionGraph, CommonJsonSupport}
import io.hydrosphere.serving.manager.repository.ApplicationRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ApplicationRepositoryImpl(
  implicit val executionContext: ExecutionContext,
  implicit val databaseService: DatabaseService
) extends ApplicationRepository with Logging with CommonJsonSupport {

  import spray.json._
  import databaseService._
  import databaseService.driver.api._
  import ApplicationRepositoryImpl.mapFromDb

  private def getServices(l: ApplicationExecutionGraph): List[Long] =
    l.stages.flatMap(s => s.services.map(c => c.serviceId))

  override def create(entity: Application): Future[Application] =
    db.run(
      Tables.Application returning Tables.Application += Tables.ApplicationRow(
        entity.id,
        entity.name,
        entity.executionGraph.toJson.toString(),
        entity.sourcesList.map(v => v.toString),
        getServices(entity.executionGraph).map(v => v.toString)
      )
    ).map(s => mapFromDb(s))

  override def get(id: Long): Future[Option[Application]] =
    db.run(
      Tables.Application
        .filter(_.id === id)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.Application
        .filter(_.id === id)
        .delete
    )

  override def all(): Future[Seq[Application]] =
    db.run(
      Tables.Application
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))

  override def update(value: Application): Future[Int] = {
    val query = for {
      serv <- Tables.Application if serv.id === value.id
    } yield (
      serv.serviceName,
      serv.executionGraph,
      serv.sourcesList,
      serv.servicesInStage
    )

    db.run(query.update(
      value.name,
      value.executionGraph.toJson.toString(),
      value.sourcesList.map(v => v.toString),
      getServices(value.executionGraph).map(v => v.toString)
    ))
  }

  override def byModelServiceIds(servicesIds: Seq[Long]): Future[Seq[Application]] =
    db.run(
      Tables.Application
        .filter(p => p.servicesInStage @> servicesIds.map(v => v.toString).toList)
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object ApplicationRepositoryImpl {

  import spray.json._

  def mapFromDb(dbType: Option[Tables.Application#TableElementType]): Option[Application] = dbType match {
    case Some(r: Tables.Application#TableElementType) =>
      Some(mapFromDb(r))
    case _ => None
  }

  def mapFromDb(dbType: Tables.Application#TableElementType): Application = {
    Application(
      id = dbType.id,
      name = dbType.serviceName,
      executionGraph = dbType.executionGraph.parseJson.convertTo[ApplicationExecutionGraph],
      sourcesList = dbType.sourcesList.map(v => v.toLong)
    )
  }
}

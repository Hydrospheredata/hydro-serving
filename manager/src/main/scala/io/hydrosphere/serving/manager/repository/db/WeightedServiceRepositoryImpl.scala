package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.model.{ServiceWeight, WeightedService}
import io.hydrosphere.serving.manager.repository.WeightedServiceRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class WeightedServiceRepositoryImpl(implicit executionContext: ExecutionContext, databaseService: DatabaseService)
  extends WeightedServiceRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import WeightedServiceRepositoryImpl.mapFromDb

  private def mapWeights(l: List[ServiceWeight]): List[String] = l.map(s => s"${s.serviceId}:${s.weight}")

  override def create(entity: WeightedService): Future[WeightedService] =
    db.run(
      Tables.WeightedService returning Tables.WeightedService += Tables.WeightedServiceRow(
        entity.id,
        entity.serviceName,
        mapWeights(entity.weights),
        entity.sourcesList.map(v => v.toString)
      )
    ).map(s => mapFromDb(s))

  override def get(id: Long): Future[Option[WeightedService]] =
    db.run(
      Tables.WeightedService
        .filter(_.id === id)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.WeightedService
        .filter(_.id === id)
        .delete
    )

  override def all(): Future[Seq[WeightedService]] =
    db.run(
      Tables.WeightedService
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))

  override def update(value: WeightedService): Future[Int] = {
    val query = for {
      serv <- Tables.WeightedService if serv.id === value.id
    } yield (
      serv.serviceName,
      serv.weights,
      serv.sourcesList
    )

    db.run(query.update(
      value.serviceName,
      mapWeights(value.weights),
      value.sourcesList.map(v => v.toString)
    ))
  }

  override def byModelServiceIds(servicesIds: Seq[Long]): Future[Seq[WeightedService]] =
    db.run(
      Tables.WeightedService
          //.filter(p => p.tags @> tags.toList) TODO
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object WeightedServiceRepositoryImpl {

  def mapFromDb(dbType: Option[Tables.WeightedService#TableElementType]): Option[WeightedService] = dbType match {
    case Some(r: Tables.WeightedService#TableElementType) =>
      Some(mapFromDb(r))
    case _ => None
  }

  def mapFromDb(dbType: Tables.WeightedService#TableElementType): WeightedService = {
    WeightedService(
      id = dbType.id,
      serviceName = dbType.serviceName,
      weights = dbType.weights.map(s => {
        val arr = s.split(":")
        ServiceWeight(
          serviceId = arr(0).toLong,
          weight = arr(1).toInt
        )
      }),
      sourcesList = dbType.sourcesList.map(v => v.toLong)
    )
  }
}

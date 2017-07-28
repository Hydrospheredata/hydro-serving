package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.repository.EndpointRepository
import io.hydrosphere.serving.model.{Endpoint, Pipeline}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class EndpointRepositoryImpl(databaseService: DatabaseService)(implicit executionContext: ExecutionContext)
  extends EndpointRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import EndpointRepositoryImpl._

  override def create(entity: Endpoint): Future[Endpoint] =
    db.run(
      Tables.Endpoint returning Tables.Endpoint += Tables.EndpointRow(
        -1,
        entity.name,
        entity.currentPipeline match {
          case Some(r) => Some(r.pipelineId)
          case _ => None
        }
      )
    ).map(s => mapFromDb(s, entity.currentPipeline))

  override def get(id: Long): Future[Option[Endpoint]] =
    db.run(
      Tables.Endpoint
        .filter(_.endpointId === id)
        .joinLeft(Tables.Pipeline)
        .on({ case (m, rt) => m.pipelineId === rt.pipelineId })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.Endpoint
        .filter(_.endpointId === id)
        .delete
    )

  override def all(): Future[Seq[Endpoint]] =
    db.run(
      Tables.Endpoint
        .joinLeft(Tables.Pipeline)
        .on({ case (m, rt) => m.pipelineId === rt.pipelineId })
        .result
    ).map(s => mapFromDb(s))
}

object EndpointRepositoryImpl {
  def mapFromDb(model: Option[(Tables.Endpoint#TableElementType, Option[Tables.Pipeline#TableElementType])]): Option[Endpoint] = model match {
    case Some(tuple) =>
      Some(mapFromDb(tuple._1, tuple._2.map(t => PipelineRepositoryImpl.mapFromDb(t))))
    case _ => None
  }

  def mapFromDb(tuples: Seq[(Tables.Endpoint#TableElementType, Option[Tables.Pipeline#TableElementType])]): Seq[Endpoint] = {
    tuples.map(tuple =>
      mapFromDb(tuple._1, tuple._2.map(t => PipelineRepositoryImpl.mapFromDb(t))))
  }

  def mapFromDb(model: Tables.Endpoint#TableElementType, pipeline: Option[Pipeline]): Endpoint = {
    Endpoint(
      endpointId = model.endpointId,
      name = model.endpointName,
      currentPipeline = pipeline
    )
  }
}
package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.repository.PipelineRepository
import io.hydrosphere.serving.model.{Pipeline, PipelineStage}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class PipelineRepositoryImpl(databaseService: DatabaseService)(implicit executionContext: ExecutionContext)
  extends PipelineRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import PipelineRepositoryImpl._

  override def create(entity: Pipeline): Future[Pipeline] =
    db.run(
      Tables.Pipeline returning Tables.Pipeline += Tables.PipelineRow(
        entity.pipelineId, entity.name, entity.stages.map(s => s"${s.serviceId}:${s.serviceName}:${s.servePath}").toList
      )
    ).map(s => mapFromDb(s))

  override def get(id: Long): Future[Option[Pipeline]] =
    db.run(
      Tables.Pipeline
        .filter(_.pipelineId === id)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.Pipeline
        .filter(_.pipelineId === id)
        .delete
    )

  override def all(): Future[Seq[Pipeline]] =
    db.run(
      Tables.Pipeline
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object PipelineRepositoryImpl {

  def mapFromDb(dbType: Option[Tables.Pipeline#TableElementType]): Option[Pipeline] = dbType match {
    case Some(r: Tables.Pipeline#TableElementType) =>
      Some(mapFromDb(r))
    case _ => None
  }

  def mapFromDb(dbType: Tables.Pipeline#TableElementType): Pipeline = {
    dbType.stages.map(s => {
      val arr = s.split(":")
      PipelineStage(
        serviceId = arr(0).toLong,
        serviceName = arr(1),
        servePath = arr(2)
      )
    })

    Pipeline(
      pipelineId = dbType.pipelineId,
      name = dbType.name,
      stages = dbType.stages.map(s => {
        val arr = s.split(":")
        PipelineStage(
          serviceId = arr(0).toLong,
          serviceName = arr(1),
          servePath = arr(2)
        )
      })
    )
  }
}
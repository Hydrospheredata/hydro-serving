package io.hydrosphere.serving.manager.repository.db

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus
import io.hydrosphere.serving.manager.model.{Model, ModelBuild, ModelBuildStatus, ModelRuntime}
import io.hydrosphere.serving.manager.repository._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ModelBuildRepositoryImpl(databaseService: DatabaseService)(implicit executionContext: ExecutionContext)
  extends ModelBuildRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import ModelBuildRepositoryImpl._

  //// status: String, statusText: Option[String] = None, logsUrl: Option[String] = None, runtimeId: Option[Long] = None)
  override def create(entity: ModelBuild): Future[ModelBuild] =
    db.run(
      Tables.ModelBuild returning Tables.ModelBuild += Tables.ModelBuildRow(
        entity.id,
        entity.model.id,
        entity.modelVersion,
        entity.started,
        entity.finished,
        entity.status.toString,
        entity.statusText,
        entity.logsUrl,
        entity.modelRuntime match {
          case Some(r) => Some(r.id)
          case _ => None
        })
    ).map(s => mapFromDb(s, Some(entity.model), entity.modelRuntime))

  override def get(id: Long): Future[Option[ModelBuild]] =
    db.run(
      Tables.ModelBuild
        .filter(_.modelBuildId === id)
        .joinLeft(Tables.Model)
        .on({ case (mb, m) => mb.modelId === m.modelId })
        .joinLeft(Tables.ModelRuntime)
        .on({ case ((mb, m), mr) => mb.runtimeId === mr.runtimeId })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.ModelBuild
        .filter(_.modelBuildId === id)
        .delete
    )

  override def all(): Future[Seq[ModelBuild]] =
    db.run(
      Tables.ModelBuild
        .joinLeft(Tables.Model)
        .on({ case (mb, m) => mb.modelId === m.modelId })
        .joinLeft(Tables.ModelRuntime)
        .on({ case ((mb, m), mr) => mb.runtimeId === mr.runtimeId })
        .result
    ).map(s => mapFromDb(s))

  override def listByModelId(id: Long): Future[Seq[ModelBuild]] =
    db.run(
      Tables.ModelBuild
        .filter(_.modelId === id)
        .joinLeft(Tables.Model)
        .on({ case (mb, m) => mb.modelId === m.modelId })
        .joinLeft(Tables.ModelRuntime)
        .on({ case ((mb, m), mr) => mb.runtimeId === mr.runtimeId })
        .result
    ).map(s => mapFromDb(s))

  override def lastByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]] =
    db.run(
      Tables.ModelBuild
        .filter(_.modelId === id)
        .sortBy(_.startedTimestamp.desc)
        .joinLeft(Tables.Model)
        .on({ case (mb, m) => mb.modelId === m.modelId })
        .joinLeft(Tables.ModelRuntime)
        .on({ case ((mb, m), mr) => mb.runtimeId === mr.runtimeId })
        .take(maximum)
        .result
    ).map(s => mapFromDb(s))

  override def finishBuild(id: Long, status: ModelBuildStatus, statusText: String, finished: LocalDateTime,
    modelRuntime: Option[ModelRuntime]): Future[Int] = {
    val query = for {
      build <- Tables.ModelBuild if build.modelBuildId === id
    } yield (build.status, build.statusText, build.finishedTimestamp, build.runtimeId)

    db.run(query.update(status.toString, Some(statusText), Some(finished), modelRuntime match {
      case Some(r) => Some(r.id)
      case _ => None
    }))
  }

}

object ModelBuildRepositoryImpl {

  def mapFromDb(model: Option[((Tables.ModelBuild#TableElementType, Option[Tables.Model#TableElementType]), Option[Tables.ModelRuntime#TableElementType])]): Option[ModelBuild] = model match {
    case Some(tuple) =>
      Some(mapFromDb(
        tuple._1._1,
        tuple._1._2.map(t => ModelRepositoryImpl.mapFromDb(t, None)),
        tuple._2.map(t => ModelRuntimeRepositoryImpl.mapFromDb(t, None))
      ))
    case _ => None
  }

  def mapFromDb(tuples: Seq[((Tables.ModelBuild#TableElementType, Option[Tables.Model#TableElementType]), Option[Tables.ModelRuntime#TableElementType])]): Seq[ModelBuild] = {
    tuples.map(tuple =>
      mapFromDb(tuple._1._1,
        tuple._1._2.map(t => ModelRepositoryImpl.mapFromDb(t, None)),
        tuple._2.map(t => ModelRuntimeRepositoryImpl.mapFromDb(t, None))))
  }

  def mapFromDb(modelBuild: Tables.ModelBuild#TableElementType, model: Option[Model], modelRuntime: Option[ModelRuntime]): ModelBuild = {
    ModelBuild(
      id = modelBuild.modelBuildId,
      model = model.getOrElse(throw new RuntimeException()),
      started = modelBuild.startedTimestamp,
      finished = modelBuild.finishedTimestamp,
      status = ModelBuildStatus.withName(modelBuild.status),
      statusText = modelBuild.statusText,
      logsUrl = modelBuild.logsUrl,
      modelRuntime = modelRuntime,
      modelVersion = modelBuild.modelVersion
    )
  }
}

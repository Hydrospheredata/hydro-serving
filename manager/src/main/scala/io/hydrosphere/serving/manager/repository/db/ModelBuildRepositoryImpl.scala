package io.hydrosphere.serving.manager.repository.db

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus.ServiceTaskStatus
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ModelBuildRepositoryImpl(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelBuildRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import ModelBuildRepositoryImpl._

  override def create(entity: ModelBuild): Future[ModelBuild] =
    db.run(
      Tables.ModelBuild returning Tables.ModelBuild += Tables.ModelBuildRow(
        modelBuildId = entity.id,
        modelId = entity.model.id,
        modelVersionId = entity.modelVersion.map(m => m.id),
        modelVersion = entity.version,
        startedTimestamp = entity.started,
        finishedTimestamp = entity.finished,
        status = entity.status.toString,
        statusText = entity.statusText,
        logsUrl = entity.logsUrl,
        script = entity.script
      )
    ).map(s => mapFromDb(s, Some(entity.model), entity.modelVersion))

  override def get(id: Long): Future[Option[ModelBuild]] =
    db.run(
      Tables.ModelBuild
        .filter(_.modelBuildId === id)
        .joinLeft(Tables.Model)
        .on({ case (mb, m) => mb.modelId === m.modelId })
        .joinLeft(Tables.ModelVersion)
        .on({ case ((mb, m), mv) => mb.modelVersionId === mv.modelVersionId })
        .result.headOption
    ).map(mapFromDb)

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
        .joinLeft(Tables.ModelVersion)
        .on({ case ((mb, m), mv) => mb.modelVersionId === mv.modelVersionId })
        .result
    ).map(mapFromDb)

  override def listByModelId(id: Long): Future[Seq[ModelBuild]] =
    db.run(
      Tables.ModelBuild
        .filter(_.modelId === id)
        .joinLeft(Tables.Model)
        .on({ case (mb, m) => mb.modelId === m.modelId })
        .joinLeft(Tables.ModelVersion)
        .on({ case ((mb, m), mv) => mb.modelVersionId === mv.modelVersionId })
        .result
    ).map(s => mapFromDb(s))

  override def lastByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]] =
    db.run(
      Tables.ModelBuild
        .filter(_.modelId === id)
        .joinLeft(Tables.Model)
        .on({ case (mb, m) => mb.modelId === m.modelId })
        .joinLeft(Tables.ModelVersion)
        .on({ case ((mb, m), mv) => mb.modelVersionId === mv.modelVersionId })
        .sortBy(_._1._1.startedTimestamp.desc)
        .take(maximum)
        .result
    ).map(mapFromDb)

  def finishBuild(id: Long, status: ServiceTaskStatus, statusText: String, finished: LocalDateTime,
    modelVersion: Option[ModelVersion]): Future[Int] = {
    val query = for {
      build <- Tables.ModelBuild if build.modelBuildId === id
    } yield (build.status, build.statusText, build.finishedTimestamp, build.modelVersionId)

    db.run(query.update(status.toString, Some(statusText), Some(finished), modelVersion.map(_.id)))
  }

  override def lastForModels(ids: Seq[Long]): Future[Seq[ModelBuild]] =
    db.run(
      Tables.ModelBuild
        .filter(_.modelId inSetBind ids)
        .sortBy(_.startedTimestamp.desc)
        .joinLeft(Tables.Model)
        .on({ case (mb, m) => mb.modelId === m.modelId })
        .joinLeft(Tables.ModelVersion)
        .on({ case ((mb, m), mv) => mb.modelVersionId === mv.modelVersionId })
        .distinctOn(_._1._1.modelId)
        .result
    ).map(s => mapFromDb(s))

  def getRunningBuild(modelId: Long, modelVersion: Long): Future[Option[ModelBuild]] = db.run {
    Tables.ModelBuild
      .filter(x => x.modelId === modelId && x.modelVersion === modelVersion)
      .filter(_.finishedTimestamp.isEmpty)
      .joinLeft(Tables.Model)
      .on({ case (mb, m) => mb.modelId === m.modelId })
      .joinLeft(Tables.ModelVersion)
      .on({ case ((mb, m), mv) => mb.modelVersionId === mv.modelVersionId })
      .result.headOption
  }.map(mapFromDb)

  override def update(entity: ModelBuild): Future[Int] = {
    val query = for {
      build <- Tables.ModelBuild if build.modelBuildId === entity.id
    } yield (
      build.finishedTimestamp,
      build.status,
      build.statusText,
      build.modelVersionId
    )
    db.run(query.update(
      entity.finished,
      entity.status.toString,
      entity.statusText,
      entity.modelVersion.map(_.id)
    ))
  }
}

object ModelBuildRepositoryImpl {

  def mapFromDb(model: Option[((Tables.ModelBuild#TableElementType, Option[Tables.Model#TableElementType]), Option[Tables.ModelVersion#TableElementType])]): Option[ModelBuild] =
    model.map {
      case ((modelBuild, maybeModel), modelVersion) =>
        val model = maybeModel.map(ModelRepositoryImpl.mapFromDb)
        mapFromDb(
          modelBuild,
          model,
          modelVersion.map(m => ModelVersionRepositoryImpl.mapFromDb(m, model))
        )
    }

  def mapFromDb(tuples: Seq[((Tables.ModelBuild#TableElementType, Option[Tables.Model#TableElementType]), Option[Tables.ModelVersion#TableElementType])]): Seq[ModelBuild] = {
    tuples.map {
      case ((modelBuild, maybeModel), modelVersion) =>
        val model = maybeModel.map(ModelRepositoryImpl.mapFromDb)
        mapFromDb(
          modelBuild,
          model,
          modelVersion.map(m => ModelVersionRepositoryImpl.mapFromDb(m, model))
        )
    }
  }

  def mapFromDb(modelBuild: Tables.ModelBuild#TableElementType, model: Option[Model], modelVersion: Option[ModelVersion]): ModelBuild = {
    ModelBuild(
      id = modelBuild.modelBuildId,
      model = model.get,
      started = modelBuild.startedTimestamp,
      finished = modelBuild.finishedTimestamp,
      status = ServiceTaskStatus.withName(modelBuild.status),
      statusText = modelBuild.statusText,
      logsUrl = modelBuild.logsUrl,
      version = modelBuild.modelVersion,
      modelVersion = modelVersion,
      script = modelBuild.script
    )
  }
}

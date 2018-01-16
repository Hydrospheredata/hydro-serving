package io.hydrosphere.serving.manager.repository.db

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus
import io.hydrosphere.serving.manager.model.ModelRuntime
import io.hydrosphere.serving.model.{ModelRuntime, RuntimeType}
import io.hydrosphere.serving.manager.model.{Model, ModelBuild, ModelBuildStatus}
import io.hydrosphere.serving.manager.repository._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ModelBuildRepositoryImpl(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelBuildRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import ModelBuildRepositoryImpl._

  //// status: String, statusText: Option[String] = None, logsUrl: Option[String] = None, runtimeId: Option[Long] = None)
  override def create(entity: ModelBuild): Future[ModelBuild] =
    db.run(
      Tables.ModelBuild returning Tables.ModelBuild += Tables.ModelBuildRow(
        modelBuildId = entity.id,
        modelId = entity.model.id,
        startedTimestamp = entity.started,
        finishedTimestamp = entity.finished,
        status = entity.status.toString,
        statusText = entity.statusText,
        logsUrl = entity.logsUrl,
        runtimeId = entity.modelRuntime.map(_.id),
        runtimeTypeId = entity.runtimeType.map(_.id),
        modelVersion = entity.modelVersion
      )
    ).map(s => mapFromDb(s, Some(entity.model), entity.modelRuntime, entity.runtimeType))

  override def get(id: Long): Future[Option[ModelBuild]] =
    db.run(
      Tables.ModelBuild
        .filter(_.modelBuildId === id)
        .joinLeft(Tables.Model)
        .on({ case (mb, m) => mb.modelId === m.modelId })
        .joinLeft(Tables.ModelRuntime)
        .on({ case ((mb, m), mr) => mb.runtimeId === mr.runtimeId })
        .joinLeft(Tables.RuntimeType)
        .on({ case (((mb, _), _), mrt) => mb.runtimeTypeId === mrt.runtimeTypeId })
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
        .joinLeft(Tables.RuntimeType)
        .on({ case (((mb, _), _), mrt) => mb.runtimeTypeId === mrt.runtimeTypeId })
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
        .joinLeft(Tables.RuntimeType)
        .on({ case (((mb, _), _), mrt) => mb.runtimeTypeId === mrt.runtimeTypeId })
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
        .joinLeft(Tables.RuntimeType)
        .on({ case (((mb, _), _), mrt) => mb.runtimeTypeId === mrt.runtimeTypeId })
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

  override def lastForModels(ids: Seq[Long]): Future[Seq[ModelBuild]] =
    db.run(
      Tables.ModelBuild
        .filter(_.modelId inSetBind ids)
        .sortBy(_.startedTimestamp.desc)
        .joinLeft(Tables.Model)
        .on({ case (mb, m) => mb.modelId === m.modelId })
        .joinLeft(Tables.ModelRuntime)
        .on({ case ((mb, _), mr) => mb.runtimeId === mr.runtimeId })
        .joinLeft(Tables.RuntimeType)
        .on({ case (((mb, _), _), mrt) => mb.runtimeTypeId === mrt.runtimeTypeId })
        .distinctOn(_._1._1._1.modelId)
        .result
    ).map(s => mapFromDb(s))
}

object ModelBuildRepositoryImpl {

  def mapFromDb(model: Option[(((Tables.ModelBuild#TableElementType, Option[Tables.Model#TableElementType]), Option[Tables.ModelRuntime#TableElementType]), Option[Tables.RuntimeType#TableElementType])]): Option[ModelBuild] =
    model.map {
      case (((modelBuild, maybeModel), maybeModelRuntime), maybeRuntimeType) =>
        mapFromDb(
          modelBuild,
          maybeModel.map(ModelRepositoryImpl.mapFromDb),
          maybeModelRuntime.map(t => ModelRuntimeRepositoryImpl.mapFromDb(t, None)),
          maybeRuntimeType.map(RuntimeTypeRepositoryImpl.mapFromDb)
        )
    }

  def mapFromDb(tuples: Seq[(((Tables.ModelBuild#TableElementType, Option[Tables.Model#TableElementType]), Option[Tables.ModelRuntime#TableElementType]), Option[Tables.RuntimeType#TableElementType])]): Seq[ModelBuild] = {
    tuples.map {
      case (((modelBuild, maybeModel), maybeModelRuntime), maybeRuntimeType) =>
        mapFromDb(
          modelBuild,
          maybeModel.map(ModelRepositoryImpl.mapFromDb),
          maybeModelRuntime.map(t => ModelRuntimeRepositoryImpl.mapFromDb(t, None)),
          maybeRuntimeType.map(RuntimeTypeRepositoryImpl.mapFromDb)
        )
    }
  }

  def mapFromDb(modelBuild: Tables.ModelBuild#TableElementType, model: Option[Model], modelRuntime: Option[ModelRuntime], runtimeType: Option[RuntimeType]): ModelBuild = {
    ModelBuild(
      id = modelBuild.modelBuildId,
      model = model.get,
      started = modelBuild.startedTimestamp,
      finished = modelBuild.finishedTimestamp,
      status = ModelBuildStatus.withName(modelBuild.status),
      statusText = modelBuild.statusText,
      logsUrl = modelBuild.logsUrl,
      modelRuntime = modelRuntime,
      modelVersion = modelBuild.modelVersion,
      runtimeType = runtimeType
    )
  }
}

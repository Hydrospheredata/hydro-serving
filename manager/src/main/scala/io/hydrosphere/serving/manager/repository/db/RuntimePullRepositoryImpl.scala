package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.db.{PullRuntime, Runtime}
import io.hydrosphere.serving.manager.repository.RuntimePullRepository
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus

import scala.concurrent.{ExecutionContext, Future}
import spray.json._
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._

class RuntimePullRepositoryImpl(
  implicit databaseService: DatabaseService,
  executionContext: ExecutionContext
) extends RuntimePullRepository {

  import spray.json._
  import databaseService._
  import databaseService.driver.api._

  private def getQ(id: Long) = {
    Tables.RuntimeBuild
      .filter(_.runtimeBuildId === id)
  }

  override def create(entity: PullRuntime): Future[PullRuntime] = db.run {
    Tables.RuntimeBuild returning Tables.RuntimeBuild += Tables.RuntimeBuildRow(
      runtimeBuildId = entity.id,
      runtimeId = entity.runtime.map(_.id),
      name = entity.name,
      version = entity.version,
      startedTimestamp = entity.startedAt,
      finishedTimestamp = entity.finishedAt,
      status = entity.status.toString,
      statusText = entity.statusText,
      logsUrl = entity.logsUrl,
      tags = entity.tags,
      configParams = entity.configParams.toJson.toString(),
      suitableModelTypes = entity.suitableModelTypes
    )
  }.map(x => RuntimePullRepositoryImpl.fromDb(x, entity.runtime))

  override def get(id: Long): Future[Option[PullRuntime]] = db.run {
    getQ(id)
      .joinLeft(Tables.Runtime)
      .on { case (rb, r) => rb.runtimeId === r.runtimeId }
      .result.headOption
  }.map(RuntimePullRepositoryImpl.fromDb)

  override def delete(id: Long): Future[Int] = db.run {
    getQ(id).delete
  }

  override def all(): Future[Seq[PullRuntime]] = db.run {
    Tables.RuntimeBuild
      .joinLeft(Tables.Runtime)
      .on { case (rb, r) => rb.runtimeId === r.runtimeId }
      .result
  }.map(RuntimePullRepositoryImpl.fromDb)

  override def getRunningPull(imageName: String, imageVersion: String): Future[Option[PullRuntime]] = db.run {
    Tables.RuntimeBuild
      .filter(_.name === imageName)
      .filter(_.version === imageVersion)
      .filter(_.status === ServiceTaskStatus.Running.toString)
      .joinLeft(Tables.Runtime)
      .on { case (rb, r) => rb.runtimeId === r.runtimeId }
      .result.headOption
  }.map(RuntimePullRepositoryImpl.fromDb)

  override def update(entity: PullRuntime): Future[Int] = {
    val q = for {
      runtime <- Tables.RuntimeBuild if runtime.runtimeBuildId === entity.id
    } yield (
      runtime.name,
      runtime.version,
      runtime.status,
      runtime.startedTimestamp,
      runtime.finishedTimestamp,
      runtime.statusText,
      runtime.logsUrl,
      runtime.runtimeId
    )
    db.run {
      q.update(
        entity.name,
        entity.version,
        entity.status.toString,
        entity.startedAt,
        entity.finishedAt,
        entity.statusText,
        entity.logsUrl,
        entity.runtime.map(_.id)
      )
    }
  }
}

object RuntimePullRepositoryImpl {
  def fromDb(x: Seq[(Tables.RuntimeBuildRow, Option[Tables.RuntimeRow])]): Seq[PullRuntime] = {
    x.map {
      case (buildRow, runtimeRow) =>
        val runtime = RuntimeRepositoryImpl.mapFromDb(runtimeRow)
        fromDb(buildRow, runtime)
    }
  }

  def fromDb(x: Option[(Tables.RuntimeBuildRow, Option[Tables.RuntimeRow])]): Option[PullRuntime] = {
    fromDb(x.toSeq).headOption
  }

  def fromDb(x: Tables.RuntimeBuildRow, r: Option[Runtime]): PullRuntime = {
    PullRuntime(
      id = x.runtimeBuildId,
      name = x.name,
      version = x.version,
      suitableModelTypes = x.suitableModelTypes,
      tags = x.tags,
      configParams = x.configParams.parseJson.convertTo[Map[String, String]],
      startedAt = x.startedTimestamp,
      finishedAt = x.finishedTimestamp,
      status = ServiceTaskStatus.withName(x.status),
      statusText = x.statusText,
      logsUrl = x.logsUrl,
      runtime = r
    )
  }

}
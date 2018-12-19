package io.hydrosphere.serving.manager.infrastructure.db.repository

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepositoryAlgebra}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.model.api.ModelType

import scala.concurrent.{ExecutionContext, Future}

class ModelVersionRepository(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelVersionRepositoryAlgebra[Future] {

  import ModelVersionRepository._
  import databaseService._
  import databaseService.driver.api._

  def joinedQ = Tables.ModelVersion
    .join(Tables.Model)
    .on((mv, m) => mv.modelId === m.modelId)
    .joinLeft(Tables.HostSelector)
    .on((m, hs) => hs.hostSelectorId === m._1.hostSelector)
    .map {
      case ((mv, m), hs) => (mv, m, hs)
    }

  override def create(entity: ModelVersion): Future[ModelVersion] =
    db.run(
      Tables.ModelVersion returning Tables.ModelVersion += Tables.ModelVersionRow(
        modelVersionId = entity.id,
        modelId = entity.model.id,
        modelVersion = entity.modelVersion,
        modelContract = entity.modelContract.toProtoString,
        createdTimestamp = entity.created,
        imageName = entity.image.name,
        imageTag = entity.image.tag,
        imageSha256 = entity.image.sha256,
        modelType = entity.modelType.toTag,
        hostSelector = entity.hostSelector.map(_.id),
        finishedTimestamp = entity.finished,
        runtimename = entity.runtime.name,
        runtimeversion = entity.runtime.tag,
        status = entity.status,
      )
    ).map(x => mapFromDb(x, entity.model, entity.hostSelector))

  override def get(id: Long): Future[Option[ModelVersion]] =
    db.run(
      joinedQ
        .filter(mv => mv._1.modelId === id)
        .result.headOption
    ).map(x => x.map(mapFromDb))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.ModelVersion
        .filter(_.modelVersionId === id)
        .delete
    )

  override def all(): Future[Seq[ModelVersion]] =
    db.run(joinedQ.result).map(_.map(mapFromDb))

  override def lastModelVersionByModel(modelId: Long, max: Int): Future[Seq[ModelVersion]] =
    db.run(
      joinedQ
        .filter(_._1.modelId === modelId)
        .sortBy(_._1.modelVersion.desc)
        .take(max)
        .result
    ).map(_.map(mapFromDb))

  override def modelVersionsByModelVersionIds(modelVersionIds: Set[Long]): Future[Seq[ModelVersion]] = {
    val action = joinedQ
      .filter {
        _._1.modelVersionId inSetBind modelVersionIds
      }
      .result

    db.run(action).map(_.map(mapFromDb))
  }

  override def modelVersionByModelAndVersion(modelId: Long, version: Long): Future[Option[ModelVersion]] =
    db.run(
      joinedQ
        .filter(r => r._1.modelId === modelId && r._1.modelVersion === version)
        .sortBy(_._1.modelVersion.desc)
        .distinctOn(_._1.modelId)
        .result.headOption
    ).map(_.map(mapFromDb))

  override def listForModel(modelId: Long): Future[Seq[ModelVersion]] = db.run {
    joinedQ
      .filter(_._1.modelId === modelId)
      .sortBy(_._1.modelVersion)
      .result
  }.map(_.map(mapFromDb))

  override def get(modelName: String, modelVersion: Long): Future[Option[ModelVersion]] = db.run {
    joinedQ
      .filter(mv => (mv._1.modelVersion === modelVersion) && (mv._2.name === modelName))
      .result.headOption
  }.map(_.map(mapFromDb))

  override def update(id: Long, entity: ModelVersion): Future[Int] = {
    val a = for {
      mv <- Tables.ModelVersion if mv.modelVersionId === id
    } yield (mv.finishedTimestamp, mv.status, mv.imageSha256)
    db.run(a.update((entity.finished, entity.status, entity.image.sha256)))
  }

  override def get(idx: Seq[Long]): Future[Seq[ModelVersion]] = {
    db.run(
      joinedQ
        .filter(_._1.modelVersionId inSetBind idx)
        .result
    ).map(_.map(mapFromDb))
  }
}

object ModelVersionRepository {
  def mapFromDb(x: (Tables.ModelVersionRow, Tables.ModelRow, Option[Tables.HostSelectorRow])): ModelVersion = {
    val m = ModelRepository.mapFromDb(x._2)
    val hs = HostSelectorRepository.mapFromDb(x._3)
    mapFromDb(x._1, m, hs)
  }

  def mapFromDb(x: Tables.ModelVersionRow, model: Model, hostSelector: Option[HostSelector]): ModelVersion = {
    ModelVersion(
      id = x.modelVersionId,
      image = DockerImage(
        name = x.imageName,
        tag = x.imageTag,
        sha256 = x.imageSha256,
      ),
      created = x.createdTimestamp,
      finished = x.finishedTimestamp,
      modelVersion = x.modelVersion,
      modelType = ModelType.fromTag(x.modelType),
      modelContract = ModelContract.fromAscii(x.modelContract),
      runtime = DockerImage(
        name = x.runtimename,
        tag = x.runtimeversion
      ),
      model = model,
      hostSelector = hostSelector,
      status = x.status
    )
  }
}
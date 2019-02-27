package io.hydrosphere.serving.manager.infrastructure.db.repository

import cats.effect.Async
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.data_profile_types.DataProfileType
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepository, ModelVersionStatus}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._
import io.hydrosphere.serving.manager.util.AsyncUtil
import spray.json._

import scala.concurrent.ExecutionContext

class DBModelVersionRepository[F[_]: Async](
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelVersionRepository[F] {

  import DBModelVersionRepository._
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

  override def create(entity: ModelVersion): F[ModelVersion] = AsyncUtil.futureAsync {
    db.run(
      Tables.ModelVersion returning Tables.ModelVersion += Tables.ModelVersionRow(
        modelVersionId = entity.id,
        modelId = entity.model.id,
        modelVersion = entity.modelVersion,
        modelContract = entity.modelContract.toJson.compactPrint,
        createdTimestamp = entity.created,
        imageName = entity.image.name,
        imageTag = entity.image.tag,
        imageSha256 = entity.image.sha256,
        hostSelector = entity.hostSelector.map(_.id),
        finishedTimestamp = entity.finished,
        runtimeName = entity.runtime.name,
        runtimeVersion = entity.runtime.tag,
        status = entity.status.toString,
        profileTypes = if (entity.profileTypes.isEmpty) None else Some(entity.profileTypes.toJson.compactPrint),
        installCommand = entity.installCommand,
        metadata = if (entity.metadata.isEmpty) None else Some(entity.metadata.toJson.compactPrint)
      )
    ).map(x => mapFromDb(x, entity.model, entity.hostSelector))
  }

  override def get(id: Long): F[Option[ModelVersion]] = AsyncUtil.futureAsync {
    db.run(
      joinedQ
        .filter(mv => mv._1.modelVersionId === id)
        .result.headOption
    ).map(x => x.map(mapFromDb))
  }

  override def delete(id: Long): F[Int] = AsyncUtil.futureAsync {
    db.run(
      Tables.ModelVersion
        .filter(_.modelVersionId === id)
        .delete
    )
  }

  override def all(): F[Seq[ModelVersion]] = AsyncUtil.futureAsync {
    db.run(joinedQ.result).map(_.map(mapFromDb))
  }

  override def lastModelVersionByModel(modelId: Long, max: Int): F[Seq[ModelVersion]] = AsyncUtil.futureAsync {
    db.run(
      joinedQ
        .filter(_._1.modelId === modelId)
        .sortBy(_._1.modelVersion.desc)
        .take(max)
        .result
    ).map(_.map(mapFromDb))
  }

  override def modelVersionsByModelVersionIds(modelVersionIds: Set[Long]): F[Seq[ModelVersion]] = AsyncUtil.futureAsync {
    val action = joinedQ
      .filter {
        _._1.modelVersionId inSetBind modelVersionIds
      }
      .result

    db.run(action).map(_.map(mapFromDb))
  }

  override def listForModel(modelId: Long): F[Seq[ModelVersion]] = AsyncUtil.futureAsync {
    db.run {
      joinedQ
        .filter(_._1.modelId === modelId)
        .sortBy(_._1.modelVersion)
        .result
    }.map(_.map(mapFromDb))
  }

  override def get(modelName: String, modelVersion: Long): F[Option[ModelVersion]] = AsyncUtil.futureAsync {
    db.run {
      joinedQ
        .filter(mv => (mv._1.modelVersion === modelVersion) && (mv._2.name === modelName))
        .result.headOption
    }.map(_.map(mapFromDb))
  }

  override def update(id: Long, entity: ModelVersion): F[Int] = AsyncUtil.futureAsync {
    val a = for {
      mv <- Tables.ModelVersion if mv.modelVersionId === id
    } yield (mv.finishedTimestamp, mv.status, mv.imageSha256)
    db.run(a.update((entity.finished, entity.status.toString, entity.image.sha256)))
  }

  override def get(idx: Seq[Long]): F[Seq[ModelVersion]] = AsyncUtil.futureAsync {
    db.run(
      joinedQ
        .filter(_._1.modelVersionId inSetBind idx)
        .result
    ).map(_.map(mapFromDb))
  }
}

object DBModelVersionRepository {
  def mapFromDb(x: (Tables.ModelVersionRow, Tables.ModelRow, Option[Tables.HostSelectorRow])): ModelVersion = {
    val m = DBModelRepository.mapFromDb(x._2)
    val hs = DBHostSelectorRepository.mapFromDb(x._3)
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
      modelContract = x.modelContract.parseJson.convertTo[ModelContract],
      runtime = DockerImage(
        name = x.runtimeName,
        tag = x.runtimeVersion
      ),
      model = model,
      hostSelector = hostSelector,
      status = ModelVersionStatus.withName(x.status),
      profileTypes = x.profileTypes.map(_.parseJson.convertTo[Map[String, DataProfileType]]).getOrElse(Map.empty),
      installCommand = x.installCommand,
      metadata = x.metadata.map(_.parseJson.convertTo[Map[String, String]]).getOrElse(Map.empty)
    )
  }
}
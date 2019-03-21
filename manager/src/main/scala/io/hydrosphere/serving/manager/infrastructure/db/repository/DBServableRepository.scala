package io.hydrosphere.serving.manager.infrastructure.db.repository

import cats.effect.Async
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.domain.servable.Servable.ConfigParams
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableRepository}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging
import spray.json._
import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}

class DBServableRepository[F[_]: Async](
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ServableRepository[F] with Logging {

  import DBServableRepository._
  import databaseService._
  import databaseService.driver.api._

  def joinedVersionQ = Tables.ModelVersion
    .join(Tables.Model)
    .on((mv, m) => mv.modelId === m.modelId)
    .joinLeft(Tables.HostSelector)
    .on((m, hs) => hs.hostSelectorId === m._1.hostSelector)
    .map {
      case ((mv, m), hs) => (mv, m, hs)
    }

  def joinedQ = Tables.Servable
    .join(joinedVersionQ)
    .on((x, a) => x.modelVersionId === a._1.modelVersionId)
    .map {
      case (s, mvv) => (s, mvv._1, mvv._2, mvv._3)
    }

  override def create(entity: Servable) = AsyncUtil.futureAsync {
    db.run(
      Tables.Servable returning Tables.Servable += Tables.ServableRow(
        serviceId = entity.id,
        serviceName = entity.serviceName,
        cloudDriverId = entity.cloudDriverId,
        modelVersionId = entity.modelVersion.id,
        statusText = entity.statusText,
        configParams = entity.configParams.toJson.compactPrint
      )
    ).map(s => mapFrom(s, entity.modelVersion))
  }

  override def get(id: Long) = AsyncUtil.futureAsync {
    db.run(
      joinedQ
        .filter(_._1.serviceId === id)
        .result.headOption
    ).map(_.map(mapEntity))
  }

  override def delete(id: Long) = AsyncUtil.futureAsync {
    db.run(
      Tables.Servable
        .filter(_.serviceId === id)
        .delete
    )
  }

  override def all() = AsyncUtil.futureAsync {
    db.run(joinedQ.result).map(_.map(x => mapEntity(x)))
  }

  override def updateCloudDriveId(serviceId: Long, cloudDriveId: Option[String]) = AsyncUtil.futureAsync {
    val query = for {
      service <- Tables.Servable if service.serviceId === serviceId
    } yield service.cloudDriverId

    db.run(query.update(cloudDriveId))
  }

  override def fetchByIds(ids: Seq[Long]) = AsyncUtil.futureAsync {
    if (ids.isEmpty) {
      Future.successful(Seq())
    } else {
      db.run(
        joinedQ
          .filter(_._1.serviceId inSetBind ids)
          .result
      ).map(_.map(mapEntity))
    }
  }
}

object DBServableRepository {
  def mapEntity(x: (Tables.ServableRow, Tables.ModelVersionRow, Tables.ModelRow, Option[Tables.HostSelectorRow])): Servable = {
    x match {
      case (service, version, model, selector) =>
        val v = DBModelVersionRepository.mapFromDb((version, model, selector))
        mapFrom(service, v)
    }
  }

  def mapFrom(service: Tables.ServableRow, modelVersion: ModelVersion): Servable = {
    Servable(
      id = service.serviceId,
      cloudDriverId = service.cloudDriverId,
      serviceName = service.serviceName,
      modelVersion = modelVersion,
      statusText = service.statusText,
      configParams = service.configParams.parseJson.convertTo[ConfigParams]
    )
  }
}
package io.hydrosphere.serving.manager.infrastructure.db.repository

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.domain.service.{Service, ServiceRepositoryAlgebra}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ServiceRepositoryImpl(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ServiceRepositoryAlgebra[Future] with Logging {

  import ServiceRepositoryImpl._
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

  def joinedQ = Tables.Service
    .join(joinedVersionQ)
    .on((x, a) => x.modelVersionId === a._1.modelVersionId)
    .map {
      case (s, mvv) => (s, mvv._1, mvv._2, mvv._3)
    }

  // status: String, statustext: Option[String] = None, configParams: List[String])
  override def create(entity: Service): Future[Service] =
    db.run(
      Tables.Service returning Tables.Service += Tables.ServiceRow(
        serviceId = entity.id,
        serviceName = entity.serviceName,
        cloudDriverId = entity.cloudDriverId,
        modelVersionId = entity.modelVersion.id,
        statusText = entity.statusText,
        configParams = entity.configParams.map { case (k, v) => s"$k=$v" }.toList
      )
    ).map(s => mapFrom(s, entity.modelVersion))

  override def get(id: Long): Future[Option[Service]] =
    db.run(
      joinedQ
        .filter(_._1.serviceId === id)
        .result.headOption
    ).map(_.map(mapEntity))

  override def delete(id: Long): Future[Int] = db.run(
    Tables.Service
      .filter(_.serviceId === id)
      .delete
  )

  override def all(): Future[Seq[Service]] =
    db.run(joinedQ.result).map(_.map(x => mapEntity(x)))

  override def updateCloudDriveId(serviceId: Long, cloudDriveId: Option[String]): Future[Int] = {
    val query = for {
      service <- Tables.Service if service.serviceId === serviceId
    } yield service.cloudDriverId

    db.run(query.update(cloudDriveId))
  }

  override def getByServiceName(serviceName: String): Future[Option[Service]] =
    db.run(
      joinedQ
        .filter(_._1.serviceName === serviceName)
        .result.headOption
    ).map(_.map(mapEntity))

  override def fetchByIds(ids: Seq[Long]): Future[Seq[Service]] = {
    if (ids.isEmpty) {
      return Future.successful(Seq())
    }
    db.run(
      joinedQ
        .filter(_._1.serviceId inSetBind ids)
        .result
    ).map(_.map(mapEntity))
  }

  override def fetchServices(serviceDescs: Set[Long]): Future[Seq[Service]] = db.run {
    joinedQ
      .filter(_._1.modelVersionId inSetBind serviceDescs)
      .result
  }.map(_.map(mapEntity))
}

object ServiceRepositoryImpl {
  def mapEntity(x: (Tables.ServiceRow, Tables.ModelVersionRow, Tables.ModelRow, Option[Tables.HostSelectorRow])): Service = {
    x match {
      case (service, version, model, selector) =>
        val v = ModelVersionRepository.mapFromDb((version, model, selector))
        mapFrom(service, v)
    }
  }

  def mapFrom(service: Tables.ServiceRow, modelVersion: ModelVersion): Service = {
    Service(
      id = service.serviceId,
      cloudDriverId = service.cloudDriverId,
      serviceName = service.serviceName,
      modelVersion = modelVersion,
      statusText = service.statusText,
      configParams = parseConfigParams(service.configParams)
    )
  }
  def parseConfigParams(strs: Seq[String]) = {
    strs.map{s =>
      val arr = s.split('=')
      arr.head -> arr.drop(1).mkString("=")
    }.toMap
  }
}

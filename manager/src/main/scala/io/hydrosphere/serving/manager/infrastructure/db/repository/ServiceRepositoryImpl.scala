package io.hydrosphere.serving.manager.infrastructure.db.repository

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
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


  // status: String, statustext: Option[String] = None, configParams: List[String])
  override def create(entity: Service): Future[Service] =
    db.run(
      Tables.Service returning Tables.Service += Tables.ServiceRow(
        serviceId = entity.id,
        serviceName = entity.serviceName,
        cloudDriverId = entity.cloudDriverId,
        modelVersionId = entity.modelVersionId,
        statusText = entity.statusText,
        configParams = entity.configParams.map { case (k, v) => s"$k=$v" }.toList
      )
    ).map(s => mapFromDb(s, entity.runtime, entity.environment, entity.model))

  override def get(id: Long): Future[Option[Service]] =
    db.run(
      Tables.Service
        .filter(_.serviceId === id)
        .joinLeft(Tables.Runtime)
        .on { case (s, r) => s.runtimeId === r.runtimeId }
        .joinLeft(Tables.Environment)
        .on { case ((s, _), e) => s.environmentId === e.environmentId }
        .joinLeft(Tables.ModelVersion)
        .on { case (((s, _), _), mv) => s.modelVersionId === mv.modelVersionId }
        .joinLeft(Tables.Model)
        .on { case ((_, mv), m) => mv.flatMap(_.modelId) === m.modelId }
        .result.headOption
    ).map(mapFromDb)

  override def delete(id: Long): Future[Int] = db.run(
    Tables.Service
      .filter(_.serviceId === id)
      .delete
  )

  override def all(): Future[Seq[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on { case (s, r) => s.runtimeId === r.runtimeId }
        .joinLeft(Tables.Environment)
        .on { case ((s, _), e) => s.environmentId === e.environmentId }
        .joinLeft(Tables.ModelVersion)
        .on { case (((s, _), _), mv) => s.modelVersionId === mv.modelVersionId }
        .joinLeft(Tables.Model)
        .on { case ((_, mv), m) => mv.flatMap(_.modelId) === m.modelId }
        .result
    ).map(mapFromDb)

  override def updateCloudDriveId(serviceId: Long, cloudDriveId: Option[String]): Future[Int] = {
    val query = for {
      service <- Tables.Service if service.serviceId === serviceId
    } yield service.cloudDriverId

    db.run(query.update(cloudDriveId))
  }

  override def getByServiceName(serviceName: String): Future[Option[Service]] =
    db.run(
      Tables.Service
        .filter(_.serviceName === serviceName)
        .joinLeft(Tables.Runtime)
        .on { case (s, r) => s.runtimeId === r.runtimeId }
        .joinLeft(Tables.Environment)
        .on { case ((s, _), e) => s.environmentId === e.environmentId }
        .joinLeft(Tables.ModelVersion)
        .on { case (((s, _), _), mv) => s.modelVersionId === mv.modelVersionId }
        .joinLeft(Tables.Model)
        .on { case ((_, mv), m) => mv.flatMap(_.modelId) === m.modelId }
        .result.headOption
    ).map(mapFromDb)

  override def fetchByIds(ids: Seq[Long]): Future[Seq[Service]] = {
    if (ids.isEmpty) {
      return Future.successful(Seq())
    }

    db.run(
      Tables.Service
        .filter(_.serviceId inSetBind ids)
        .joinLeft(Tables.Runtime)
        .on { case (s, r) => s.runtimeId === r.runtimeId }
        .joinLeft(Tables.Environment)
        .on { case ((s, _), e) => s.environmentId === e.environmentId }
        .joinLeft(Tables.ModelVersion)
        .on { case (((s, _), _), mv) => s.modelVersionId === mv.modelVersionId }
        .joinLeft(Tables.Model)
        .on { case ((_, mv), m) => mv.flatMap(_.modelId) === m.modelId }
        .result
    ).map(mapFromDb)
  }

  override def getByModelIds(modelIds: Seq[Long]): Future[Seq[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on { case (s, r) => s.runtimeId === r.runtimeId }
        .joinLeft(Tables.Environment)
        .on { case ((s, _), e) => s.environmentId === e.environmentId }
        .joinLeft(Tables.ModelVersion)
        .on { case (((s, _), _), mv) => s.modelVersionId === mv.modelVersionId }
        .joinLeft(Tables.Model)
        .on { case ((_, mv), m) => mv.flatMap(_.modelId) === m.modelId }
        .filter { case ((_, mv), _) => mv.flatMap(_.modelId) inSetBind modelIds }
        .result
    ).map(mapFromDb)

  override def getByRuntimeIds(runtimeIds: Set[Long]): Future[Seq[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on { case (s, r) => s.runtimeId === r.runtimeId }
        .joinLeft(Tables.Environment)
        .on { case ((s, _), e) => s.environmentId === e.environmentId }
        .joinLeft(Tables.ModelVersion)
        .on { case (((s, _), _), mv) => s.modelVersionId === mv.modelVersionId }
        .joinLeft(Tables.Model)
        .on { case ((((_, _), _), mv), m) => mv.flatMap(_.modelId) === m.modelId }
        .filter { case ((((_, r), _), _), _) => r.map(_.runtimeId) inSetBind runtimeIds }
        .result
    ).map(mapFromDb)

  override def getLastServiceByModelName(modelName: String): Future[Option[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on { case (s, r) => s.runtimeId === r.runtimeId }
        .joinLeft(Tables.Environment)
        .on { case ((s, _), e) => s.environmentId === e.environmentId }
        .joinLeft(Tables.ModelVersion)
        .on { case (((s, _), _), mv) => s.modelVersionId === mv.modelVersionId }
        .joinLeft(Tables.Model)
        .on { case ((_, mv), m) => mv.flatMap(_.modelId) === m.modelId }
        .filter { case ((_, mv), _) => mv.map(_.modelName) === modelName }
        .result.headOption
    ).map(mapFromDb)

  override def getLastServiceByModelNameAndVersion(modelName: String, modelVersion: Long): Future[Option[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on { case (s, r) => s.runtimeId === r.runtimeId }
        .joinLeft(Tables.Environment)
        .on { case ((s, _), e) => s.environmentId === e.environmentId }
        .joinLeft(Tables.ModelVersion)
        .on { case (((s, _), _), mv) => s.modelVersionId === mv.modelVersionId }
        .joinLeft(Tables.Model)
        .on { case ((_, mv), m) => mv.flatMap(_.modelId) === m.modelId }
        .filter { case ((_, mv), _) => mv.map(_.modelName) === modelName && mv.map(_.modelVersion) === modelVersion }
        .result.headOption
    ).map(mapFromDb)

  override def getByModelVersionIds(modelIds: Seq[Long]): Future[Seq[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on { case (s, r) => s.runtimeId === r.runtimeId }
        .joinLeft(Tables.Environment)
        .on { case ((s, _), e) => s.environmentId === e.environmentId }
        .joinLeft(Tables.ModelVersion)
        .on { case (((s, _), _), mv) => s.modelVersionId === mv.modelVersionId }
        .joinLeft(Tables.Model)
        .on { case ((_, mv), m) => mv.flatMap(_.modelId) === m.modelId }
        .filter { case ((_, mv), _) => mv.map(_.modelVersionId) inSetBind modelIds }
        .result
    ).map(mapFromDb)

  override def fetchServices(serviceDescs: Set[ServiceKeyDescription]): Future[Seq[Service]] = {
    val runtimeIdx = serviceDescs.map(_.runtimeId)
    for {
      services <- getByRuntimeIds(runtimeIdx)
    } yield {
      val filtered = services.filter { service =>
        serviceDescs.contains(service.toServiceKeyDescription)
      }
      logger.debug(s"services=$services")
      logger.debug(s"filtered=$filtered")
      filtered
    }
  }

  override def update(entity: Service): Future[Int] = ???
}

object ServiceRepositoryImpl {

  def mapFromDb(model: Option[((((Tables.Service#TableElementType, Option[Tables.Runtime#TableElementType]), Option[Tables.Environment#TableElementType]),
    Option[Tables.ModelVersion#TableElementType]), Option[Tables.Model#TableElementType])]): Option[Service] =
    model.map(mapFromDb)

  def mapFromDb(tuples: Seq[
    ((((Tables.Service#TableElementType, Option[Tables.Runtime#TableElementType]), Option[Tables.Environment#TableElementType]),
      Option[Tables.ModelVersion#TableElementType]), Option[Tables.Model#TableElementType])]): Seq[Service] = {
    tuples.map(mapFromDb)
  }

  def mapFromDb(tuple: (Tables.Service#TableElementType, Tables.ModelVersion#TableElementType)): Service = {
    tuple match {
      case (service, modelVersion) =>
        mapFromDb(
          service,
          ModelVersionRepository.mapFromDb(modelVersion)
        )
    }
  }

  def mapFromDb(
    model: Tables.Service#TableElementType,
    modelVersion: ModelVersion
  ): Service = {
    Service(
      id = model.serviceId,
      serviceName = model.serviceName,
      cloudDriverId = model.cloudDriverId,
      model = modelVersion,
      statusText = model.statusText,
      configParams = model.configParams.map(s => {
        val arr = s.split('=')
        arr.head -> arr.drop(1).mkString("=")
      }).toMap
    )
  }
}

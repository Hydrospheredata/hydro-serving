package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.repository.ServiceRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ServiceRepositoryImpl(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ServiceRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import ServiceRepositoryImpl._


  // status: String, statustext: Option[String] = None, configParams: List[String])
  override def create(entity: Service): Future[Service] =
    db.run(
      Tables.Service returning Tables.Service += Tables.ServiceRow(
        serviceId = entity.id,
        serviceName = entity.serviceName,
        cloudDriverId = entity.cloudDriverId,
        runtimeId = entity.runtime.id,
        environmentId = entity.environment.map(e => e.id),
        modelVersionId = entity.model.map(m => m.id),
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

  override def fetchServices(services: Set[ServiceKeyDescription]): Future[Seq[Service]] = {
    getByRuntimeIds(services.map(k => {
      k.runtimeId
    })).map(s => {
      s.filter(service => {
        services.contains(service.toServiceKeyDescription)
      })
    })
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

  def mapFromDb(tuple:
  ((((Tables.Service#TableElementType, Option[Tables.Runtime#TableElementType]), Option[Tables.Environment#TableElementType]),
    Option[Tables.ModelVersion#TableElementType]), Option[Tables.Model#TableElementType])): Service = {
    tuple match {
      case ((((service, runtime), environment), modelVersion), model) =>
        mapFromDb(
          service,
          RuntimeRepositoryImpl.mapFromDb(
            runtime.getOrElse(throw new RuntimeException(s"Can't find Runtime for service ${service.serviceId}"))
          ),
          EnvironmentRepositoryImpl.mapFromDb(environment),
          modelVersion.map(s =>
            ModelVersionRepositoryImpl.mapFromDb(s, ModelRepositoryImpl.mapFromDb(model))
          )
        )
    }
  }

  def mapFromDb(
    model: Tables.Service#TableElementType,
    runtime: Runtime,
    environment: Option[Environment],
    modelVersion: Option[ModelVersion]
  ): Service = {
    Service(
      id = model.serviceId,
      serviceName = model.serviceName,
      cloudDriverId = model.cloudDriverId,
      runtime = runtime,
      environment = environment,
      model = modelVersion,
      statusText = model.statusText,
      configParams = model.configParams.map(s => {
        val arr = s.split('=')
        arr.head -> arr.drop(1).mkString("=")
      }).toMap
    )
  }
}

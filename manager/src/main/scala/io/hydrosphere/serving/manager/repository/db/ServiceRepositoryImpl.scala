package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.repository.ServiceRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
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
        status = entity.status,
        statusText = entity.statusText,
        configParams = entity.configParams.map { case (k, v) => s"$k=$v" }.toList
      )
    ).map(s => mapFromDb(s, entity.runtime, entity.environment, entity.model))

  override def get(id: Long): Future[Option[Service]] =
    db.run(
      Tables.Service
        .filter(_.serviceId === id)
        .joinLeft(Tables.Runtime)
        .on({ case (s, r) => s.runtimeId === r.runtimeId })
        .joinLeft(Tables.Environment)
        .on({ case ((s, r), e) => s.environmentId === e.environmentId })
        .joinLeft(Tables.ModelVersion)
        .on({ case (((s, r), e), mv) => s.modelVersionId === mv.modelVersionId })
        .joinLeft(Tables.Model)
        .on({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelId) === m.modelId })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def delete(id: Long): Future[Int] = db.run(
    Tables.Service
      .filter(_.serviceId === id)
      .delete
  )

  override def all(): Future[Seq[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on({ case (s, r) => s.runtimeId === r.runtimeId })
        .joinLeft(Tables.Environment)
        .on({ case ((s, r), e) => s.environmentId === e.environmentId })
        .joinLeft(Tables.ModelVersion)
        .on({ case (((s, r), e), mv) => s.modelVersionId === mv.modelVersionId })
        .joinLeft(Tables.Model)
        .on({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelId) === m.modelId })
        .result
    ).map(s => mapFromDb(s))

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
        .on({ case (s, r) => s.runtimeId === r.runtimeId })
        .joinLeft(Tables.Environment)
        .on({ case ((s, r), e) => s.environmentId === e.environmentId })
        .joinLeft(Tables.ModelVersion)
        .on({ case (((s, r), e), mv) => s.modelVersionId === mv.modelVersionId })
        .joinLeft(Tables.Model)
        .on({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelId) === m.modelId })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def fetchByIds(ids: Seq[Long]): Future[Seq[Service]] = {
    if (ids.isEmpty) {
      return Future.successful(Seq())
    }

    db.run(
      Tables.Service
        .filter(_.serviceId inSetBind ids)
        .joinLeft(Tables.Runtime)
        .on({ case (s, r) => s.runtimeId === r.runtimeId })
        .joinLeft(Tables.Environment)
        .on({ case ((s, r), e) => s.environmentId === e.environmentId })
        .joinLeft(Tables.ModelVersion)
        .on({ case (((s, r), e), mv) => s.modelVersionId === mv.modelVersionId })
        .joinLeft(Tables.Model)
        .on({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelId) === m.modelId })
        .result
    ).map(m => mapFromDb(m))
  }

  override def getByModelIds(modelIds: Seq[Long]): Future[Seq[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on({ case (s, r) => s.runtimeId === r.runtimeId })
        .joinLeft(Tables.Environment)
        .on({ case ((s, r), e) => s.environmentId === e.environmentId })
        .joinLeft(Tables.ModelVersion)
        .on({ case (((s, r), e), mv) => s.modelVersionId === mv.modelVersionId })
        .joinLeft(Tables.Model)
        .on({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelId) === m.modelId })
        .filter({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelId) inSetBind modelIds })
        .result
    ).map(m => mapFromDb(m))

  override def getByModelRuntimeIds(runtimeIds: Seq[Long]): Future[Seq[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on({ case (s, r) => s.runtimeId === r.runtimeId })
        .joinLeft(Tables.Environment)
        .on({ case ((s, r), e) => s.environmentId === e.environmentId })
        .joinLeft(Tables.ModelVersion)
        .on({ case (((s, r), e), mv) => s.modelVersionId === mv.modelVersionId })
        .joinLeft(Tables.Model)
        .on({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelId) === m.modelId })
        .filter({ case ((((s, r), e), mv), m) => r.flatMap(_.runtimeId) inSetBind runtimeIds })
        .result
    ).map(m => mapFromDb(m))

  override def getLastServiceByModelName(modelName: String): Future[Option[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on({ case (s, r) => s.runtimeId === r.runtimeId })
        .joinLeft(Tables.Environment)
        .on({ case ((s, r), e) => s.environmentId === e.environmentId })
        .joinLeft(Tables.ModelVersion)
        .on({ case (((s, r), e), mv) => s.modelVersionId === mv.modelVersionId })
        .joinLeft(Tables.Model)
        .on({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelId) === m.modelId })
        .filter({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelName) === modelName })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def getLastServiceByModelNameAndVersion(modelName: String, modelVersion: Long): Future[Option[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on({ case (s, r) => s.runtimeId === r.runtimeId })
        .joinLeft(Tables.Environment)
        .on({ case ((s, r), e) => s.environmentId === e.environmentId })
        .joinLeft(Tables.ModelVersion)
        .on({ case (((s, r), e), mv) => s.modelVersionId === mv.modelVersionId })
        .joinLeft(Tables.Model)
        .on({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelId) === m.modelId })
        .filter({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelName) === modelName && mv.flatMap(_.modelVersion) === modelVersion })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def getByModelVersionIds(modelIds: Seq[Long]): Future[Seq[Service]] =
    db.run(
      Tables.Service
        .joinLeft(Tables.Runtime)
        .on({ case (s, r) => s.runtimeId === r.runtimeId })
        .joinLeft(Tables.Environment)
        .on({ case ((s, r), e) => s.environmentId === e.environmentId })
        .joinLeft(Tables.ModelVersion)
        .on({ case (((s, r), e), mv) => s.modelVersionId === mv.modelVersionId })
        .joinLeft(Tables.Model)
        .on({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelId) === m.modelId })
        .filter({ case ((((s, r), e), mv), m) => mv.flatMap(_.modelVersionId) inSetBind modelIds })
        .result
    ).map(m => mapFromDb(m))
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
      status = model.status,
      statusText = model.statusText,
      configParams = model.configParams.map(s => {
        val arr = s.split('=')
        arr.head -> arr.drop(1).mkString("=")
      }).toMap
    )
  }
}

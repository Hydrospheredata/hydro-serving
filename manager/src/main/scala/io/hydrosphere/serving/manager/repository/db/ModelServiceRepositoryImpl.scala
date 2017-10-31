package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.model._
import io.hydrosphere.serving.manager.repository.ModelServiceRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ModelServiceRepositoryImpl(
  implicit val executionContext: ExecutionContext,
  implicit val databaseService: DatabaseService
)
  extends ModelServiceRepository with Logging {

  import databaseService._
  import databaseService.driver.api._
  import ModelServiceRepositoryImpl._


  override def create(entity: ModelService): Future[ModelService] =
    db.run(
      Tables.ModelService returning Tables.ModelService += Tables.ModelServiceRow(
        entity.serviceId,
        entity.serviceName,
        entity.cloudDriverId,
        entity.modelRuntime.id,
        entity.status,
        entity.statusText,
        entity.configParams.map { case (k, v) => s"$k=$v" }.toList
      )
    ).map(s => mapFromDb(s, Some(entity.modelRuntime)))

  override def get(id: Long): Future[Option[ModelService]] =
    db.run(
      Tables.ModelService
        .filter(_.serviceId === id)
        .joinLeft(Tables.ModelRuntime)
        .on({ case (ms, mr) => ms.runtimeId === mr.runtimeId })
        .joinLeft(Tables.RuntimeType)
        .on({ case ((ms, mr), rt) => mr.flatMap(_.runtimeTypeId) === rt.runtimeTypeId })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def delete(id: Long): Future[Int] = db.run(
    Tables.ModelService
      .filter(_.serviceId === id)
      .delete
  )

  override def all(): Future[Seq[ModelService]] =
    db.run(
      Tables.ModelService
        .joinLeft(Tables.ModelRuntime)
        .on({ case (ms, mr) => ms.runtimeId === mr.runtimeId })
        .joinLeft(Tables.RuntimeType)
        .on({ case ((ms, mr), rt) => mr.flatMap(_.runtimeTypeId) === rt.runtimeTypeId })
        .result
    ).map(s => mapFromDb(s))

  override def updateCloudDriveId(serviceId: Long, cloudDriveId: Option[String]): Future[Int] = {
    val query = for {
      service <- Tables.ModelService if service.serviceId === serviceId
    } yield service.cloudDriverId

    db.run(query.update(cloudDriveId))
  }

  override def getByServiceName(serviceName: String): Future[Option[ModelService]] =
    db.run(
      Tables.ModelService
        .filter(_.serviceName === serviceName)
        .joinLeft(Tables.ModelRuntime)
        .on({ case (ms, mr) => ms.runtimeId === mr.runtimeId })
        .joinLeft(Tables.RuntimeType)
        .on({ case ((ms, mr), rt) => mr.flatMap(_.runtimeTypeId) === rt.runtimeTypeId })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def fetchByIds(ids: Seq[Long]): Future[Seq[ModelService]] = {
    if (ids.isEmpty) {
      return Future.successful(Seq())
    }

    db.run(
      Tables.ModelService
        .filter(_.serviceId inSetBind ids)
        .joinLeft(Tables.ModelRuntime)
        .on({ case (ms, mr) => ms.runtimeId === mr.runtimeId })
        .joinLeft(Tables.RuntimeType)
        .on({ case ((ms, mr), rt) => mr.flatMap(_.runtimeTypeId) === rt.runtimeTypeId })
        .result
    ).map(m => mapFromDb(m))
  }

  override def getByModelIds(modelIds: Seq[Long]): Future[Seq[ModelService]] =
    db.run(
      Tables.ModelService
        .joinLeft(Tables.ModelRuntime)
        .on({ case (ms, mr) => ms.runtimeId === mr.runtimeId })
        .joinLeft(Tables.RuntimeType)
        .on({ case ((ms, mr), rt) => mr.flatMap(_.runtimeTypeId) === rt.runtimeTypeId })
        .filter({ case ((ms, mr), rt) => mr.flatMap(_.modelId) inSetBind modelIds })
        .result
    ).map(m => mapFromDb(m))

  override def getByModelRuntimeIds(runtimeIds: Seq[Long]): Future[Seq[ModelService]] =
    db.run(
      Tables.ModelService
        .joinLeft(Tables.ModelRuntime)
        .on({ case (ms, mr) => ms.runtimeId === mr.runtimeId })
        .joinLeft(Tables.RuntimeType)
        .on({ case ((ms, mr), rt) => mr.flatMap(_.runtimeTypeId) === rt.runtimeTypeId })
        .filter({ case ((ms, mr), rt) => mr.flatMap(_.runtimeId) inSetBind runtimeIds })
        .result
    ).map(m => mapFromDb(m))

  override def getLastModelServiceByModelName(modelName: String): Future[Option[ModelService]] =
    db.run(
      Tables.ModelService
        .joinLeft(Tables.ModelRuntime)
        .on({ case (ms, mr) => ms.runtimeId === mr.runtimeId })
        .joinLeft(Tables.RuntimeType)
        .on({ case ((ms, mr), rt) => mr.flatMap(_.runtimeTypeId) === rt.runtimeTypeId })
        .filter({ case ((ms, mr), rt) => mr.flatMap(_.modelname) === modelName })
        .result.headOption
    ).map(m => mapFromDb(m))

  override def getLastModelServiceByModelNameAndVersion(modelName: String, modelVersion: String): Future[Option[ModelService]] =
    db.run(
      Tables.ModelService
        .joinLeft(Tables.ModelRuntime)
        .on({ case (ms, mr) => ms.runtimeId === mr.runtimeId })
        .joinLeft(Tables.RuntimeType)
        .on({ case ((ms, mr), rt) => mr.flatMap(_.runtimeTypeId) === rt.runtimeTypeId })
        .filter({ case ((ms, mr), rt) => mr.flatMap(_.modelname) === modelName && mr.flatMap(_.modelversion) === modelVersion })
        .result.headOption
    ).map(m => mapFromDb(m))
}

object ModelServiceRepositoryImpl {

  def mapFromDb(model: Option[(
    (Tables.ModelService#TableElementType, Option[Tables.ModelRuntime#TableElementType]),
      Option[Tables.RuntimeType#TableElementType]
    )]): Option[ModelService] = model match {
    case Some(tuple) =>
      Some(mapFromDb(tuple._1._1,
        tuple._1._2.map(
          t => ModelRuntimeRepositoryImpl.mapFromDb(t, RuntimeTypeRepositoryImpl.mapFromDb(tuple._2))
        )
      ))
    case _ => None
  }

  def mapFromDb(tuples: Seq[(
    (Tables.ModelService#TableElementType, Option[Tables.ModelRuntime#TableElementType]),
      Option[Tables.RuntimeType#TableElementType]
    )]): Seq[ModelService] = {
    tuples.map(tuple =>
      mapFromDb(tuple._1._1,
        tuple._1._2.map(
          t => ModelRuntimeRepositoryImpl.mapFromDb(t, RuntimeTypeRepositoryImpl.mapFromDb(tuple._2))
        )
      )
    )
  }

  def mapFromDb(model: Tables.ModelService#TableElementType, modelRuntime: Option[ModelRuntime]): ModelService = {
    ModelService(
      serviceId = model.serviceId,
      serviceName = model.serviceName,
      cloudDriverId = model.cloudDriverId,
      modelRuntime = modelRuntime.getOrElse(throw new RuntimeException("Can't find ModelRuntime for service")),
      status = model.status,
      statusText = model.statustext,
      configParams = model.configParams.map(s => {
        val arr = s.split('=')
        arr.head -> arr.drop(1).mkString("=")
      }).toMap
    )
  }
}

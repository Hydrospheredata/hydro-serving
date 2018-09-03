package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model.DataProfileFields
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Model
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
import io.hydrosphere.serving.manager.repository.ModelRepository
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class ModelRepositoryImpl(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelRepository with Logging {

  import ModelRepositoryImpl._
  import databaseService._
  import databaseService.driver.api._

  override def create(entity: Model): Future[Model] =
    db.run(
      Tables.Model returning Tables.Model += Tables.ModelRow(
        modelId = entity.id,
        name = entity.name,
        modelType = entity.modelType.toTag,
        modelContract = entity.modelContract.toProtoString,
        description = entity.description,
        createdTimestamp = entity.created,
        updatedTimestamp = entity.updated,
        dataProfileFields = entity.dataProfileTypes.map(_.toJson.toString)
      )
    ).map(mapFromDb)


  override def get(id: Long): Future[Option[Model]] =
    db.run(
      Tables.Model
        .filter(_.modelId === id)
        .result.headOption
    ).map(mapFromDb)

  override def get(name: String): Future[Option[Model]] =
    db.run(
      Tables.Model
        .filter(_.name === name)
        .result.headOption
    ).map(mapFromDb)

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.Model
        .filter(_.modelId === id)
        .delete
    )

  override def all(): Future[Seq[Model]] =
    db.run(
      Tables.Model
        .result
    ).map(mapFromDb)

  override def update(model: Model): Future[Int] = {
    val query = for {
      models <- Tables.Model if models.modelId === model.id
    } yield (
      models.name,
      models.modelType,
      models.description,
      models.updatedTimestamp,
      models.modelContract,
      models.dataProfileFields
    )

    db.run(query.update(
      model.name,
      model.modelType.toTag,
      model.description,
      model.updated,
      model.modelContract.toProtoString,
      model.dataProfileTypes.map(_.toJson.toString)
    ))
  }

  override def fetchByModelType(types: Seq[ModelType]): Future[Seq[Model]] =
    db.run(
      Tables.Model
        .filter(_.modelType inSetBind types.map(p => p.toTag))
        .result
    ).map(mapFromDb)

  override def getMany(ids: Set[Long]): Future[Seq[Model]] = {
    db.run(
      Tables.Model
        .filter(_.modelId inSetBind ids)
        .result
    ).map(mapFromDb)
  }
}

object ModelRepositoryImpl {

  def mapFromDb(model: Option[Tables.Model#TableElementType]): Option[Model] =
    model.map(mapFromDb)

  def mapFromDb(models: Seq[Tables.Model#TableElementType]): Seq[Model] =
    models.map { model =>
      mapFromDb(model)
    }

  def mapFromDb(model: Tables.Model#TableElementType): Model =
    Model(
      id = model.modelId,
      name = model.name,
      modelType = ModelType.fromTag(model.modelType),
      description = model.description,
      modelContract = ModelContract.fromAscii(model.modelContract),
      created = model.createdTimestamp,
      updated = model.updatedTimestamp,
      dataProfileTypes = model.dataProfileFields.map(_.parseJson.convertTo[DataProfileFields])
    )
}
package io.hydrosphere.serving.manager.infrastructure.db.repository

import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.model.{Model, ModelRepositoryAlgebra}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ModelRepository(
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelRepositoryAlgebra[Future] with Logging {

  import ModelRepository._
  import databaseService._
  import databaseService.driver.api._

  override def create(entity: Model): Future[Model] =
    db.run(
      Tables.Model returning Tables.Model += Tables.ModelRow(
        modelId = entity.id,
        name = entity.name,
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
    } yield models.name

    db.run(query.update(model.name))
  }

  override def getMany(ids: Set[Long]): Future[Seq[Model]] = {
    db.run(
      Tables.Model
        .filter(_.modelId inSetBind ids)
        .result
    ).map(mapFromDb)
  }
}

object ModelRepository {

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
    )
}
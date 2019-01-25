package io.hydrosphere.serving.manager.infrastructure.db.repository

import cats.effect.Async
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.model.{Model, ModelRepository}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

class DBModelRepository[F[_]: Async](
  implicit executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ModelRepository[F] with Logging {

  import DBModelRepository._
  import databaseService._
  import databaseService.driver.api._

  override def create(entity: Model): F[Model] = AsyncUtil.futureAsync {
    db.run(
      Tables.Model returning Tables.Model += Tables.ModelRow(
        modelId = entity.id,
        name = entity.name,
      )
    ).map(mapFromDb)
  }


  override def get(id: Long): F[Option[Model]] = AsyncUtil.futureAsync {
    db.run(
      Tables.Model
        .filter(_.modelId === id)
        .result.headOption
    ).map(mapFromDb)
  }

  override def get(name: String): F[Option[Model]] = AsyncUtil.futureAsync {
    db.run(
      Tables.Model
        .filter(_.name === name)
        .result.headOption
    ).map(mapFromDb)
  }

  override def delete(id: Long): F[Int] = AsyncUtil.futureAsync {
    db.run(
      Tables.Model
        .filter(_.modelId === id)
        .delete
    )
  }

  override def all(): F[Seq[Model]] = AsyncUtil.futureAsync {
    db.run(
      Tables.Model
        .result
    ).map(mapFromDb)
  }

  override def update(model: Model): F[Int] = AsyncUtil.futureAsync {
    val query = for {
      models <- Tables.Model if models.modelId === model.id
    } yield models.name

    db.run(query.update(model.name))
  }

  override def getMany(ids: Set[Long]): F[Seq[Model]] = AsyncUtil.futureAsync {
    db.run(
      Tables.Model
        .filter(_.modelId inSetBind ids)
        .result
    ).map(mapFromDb)
  }
}

object DBModelRepository {

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
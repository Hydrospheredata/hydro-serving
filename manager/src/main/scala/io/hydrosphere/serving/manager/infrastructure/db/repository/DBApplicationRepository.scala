package io.hydrosphere.serving.manager.infrastructure.db.repository

import cats.effect.Async
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.application._
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.concurrent.ExecutionContext

class DBApplicationRepository[F[_]: Async](
  implicit val executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ApplicationRepository[F] with Logging with CompleteJsonProtocol {

  import DBApplicationRepository._
  import databaseService._
  import databaseService.driver.api._

  override def create(entity: Application): F[Application] = AsyncUtil.futureAsync {
    db.run(
      Tables.Application returning Tables.Application += Tables.ApplicationRow(
        id = entity.id,
        applicationName = entity.name,
        namespace = entity.namespace,
        status = entity.status.toString,
        applicationContract = entity.signature.toProtoString,
        executionGraph = entity.executionGraph.toJson.toString(),
        servablesInStage = entity.executionGraph.stages.flatMap(s => s.modelVariants.map(_.modelVersion.id.toString)).toList,
        kafkaStreams = entity.kafkaStreaming.map(p => p.toJson.toString()).toList
      )
    ).map(s => mapFromDb(s))
  }

  override def get(id: Long): F[Option[Application]] = AsyncUtil.futureAsync {
    db.run(
      Tables.Application
        .filter(_.id === id)
        .result.headOption
    ).map(s => mapFromDb(s))
  }

  override def delete(id: Long): F[Int] = AsyncUtil.futureAsync {
    db.run(
      Tables.Application
        .filter(_.id === id)
        .delete
    )
  }

  override def all(): F[Seq[Application]] = AsyncUtil.futureAsync {
    db.run(
      Tables.Application
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
  }

  override def update(value: Application): F[Int] = AsyncUtil.futureAsync {
    val query = for {
      serv <- Tables.Application if serv.id === value.id
    } yield (
      serv.applicationName,
      serv.executionGraph,
      serv.servablesInStage,
      serv.kafkaStreams,
      serv.namespace,
      serv.applicationContract,
      serv.status
    )

    db.run(query.update((
      value.name,
      value.executionGraph.toJson.toString(),
      value.executionGraph.stages.flatMap(s => s.modelVariants.map(_.modelVersion.id.toString)).toList,
      value.kafkaStreaming.map(_.toJson.toString).toList,
      value.namespace,
      value.signature.toProtoString,
      value.status.toString
    )))
  }

  override def applicationsWithCommonServices(versionIdx: Set[Long], applicationId: Long): F[Seq[Application]] = AsyncUtil.futureAsync {
    db.run(
      Tables.Application
        .filter { p =>
          p.servablesInStage @> versionIdx.map(_.toString).toList && p.id =!= applicationId
        }
        .result
    ).map(s => s.map(mapFromDb))
  }

  override def findVersionsUsage(versionIdx: Long): F[Seq[Application]] = AsyncUtil.futureAsync {
    db.run {
      Tables.Application
        .filter(a => a.servablesInStage @> List(versionIdx.toString))
        .result
    }.map(_.map(mapFromDb))
  }

  override def get(name: String): F[Option[Application]] = AsyncUtil.futureAsync {
    db.run(
      Tables.Application
        .filter(_.applicationName === name)
        .result.headOption
    ).map(s => mapFromDb(s))
  }
}

object DBApplicationRepository extends CompleteJsonProtocol {

  import spray.json._

  def mapFromDb(dbType: Option[Tables.Application#TableElementType]): Option[Application] =
    dbType.map(r => mapFromDb(r))

  def mapFromDb(dbType: Tables.Application#TableElementType): Application = {
    Application(
      id = dbType.id,
      name = dbType.applicationName,
      executionGraph = dbType.executionGraph.parseJson.convertTo[ApplicationExecutionGraph],
      signature = ModelSignature.fromAscii(dbType.applicationContract),
      kafkaStreaming = dbType.kafkaStreams.map(p => p.parseJson.convertTo[ApplicationKafkaStream]),
      namespace = dbType.namespace,
      status = ApplicationStatus.withName(dbType.status)
    )
  }
}
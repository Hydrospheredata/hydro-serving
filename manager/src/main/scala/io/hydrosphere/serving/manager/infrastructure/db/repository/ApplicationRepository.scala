package io.hydrosphere.serving.manager.infrastructure.db.repository

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.application._
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class ApplicationRepository(
  implicit val executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ApplicationRepositoryAlgebra[Future] with Logging with CompleteJsonProtocol {

  import ApplicationRepository._
  import databaseService._
  import databaseService.driver.api._

  override def create(entity: Application): Future[Application] =
    db.run(
      Tables.Application returning Tables.Application += Tables.ApplicationRow(
        id = entity.id,
        applicationName = entity.name,
        namespace = entity.namespace,
        applicationContract = entity.signature.toProtoString,
        executionGraph = entity.executionGraph.toJson.toString(),
        servicesInStage = entity.executionGraph.stages.flatMap(s => s.services.map(_.modelVersion.id.toString)).toList,
        kafkaStreams = entity.kafkaStreaming.map(p => p.toJson.toString())
      )
    ).map(s => mapFromDb(s))

  override def get(id: Long): Future[Option[Application]] =
    db.run(
      Tables.Application
        .filter(_.id === id)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def delete(id: Long): Future[Int] =
    db.run(
      Tables.Application
        .filter(_.id === id)
        .delete
    )

  override def all(): Future[Seq[Application]] =
    db.run(
      Tables.Application
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))

  override def update(value: Application): Future[Int] = {
    val query = for {
      serv <- Tables.Application if serv.id === value.id
    } yield (
      serv.applicationName,
      serv.executionGraph,
      serv.servicesInStage,
      serv.kafkaStreams,
      serv.namespace,
      serv.applicationContract
    )

    db.run(query.update(
      value.name,
      value.executionGraph.toJson.toString(),
      value.executionGraph.stages.flatMap(s => s.services.map(_.modelVersion.id.toString)).toList,
      value.kafkaStreaming.map(_.toJson.toString),
      value.namespace,
      value.signature.toProtoString
    ))
  }

  override def getByName(name: String): Future[Option[Application]] =
    db.run(
      Tables.Application
        .filter(_.applicationName === name)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def getKeysNotInApplication(versionIdx: Set[Long], applicationId: Long): Future[Seq[Application]] =
    db.run(
      Tables.Application
        .filter { p =>
          p.servicesInStage @> versionIdx.map(_.toString).toList && p.id =!= applicationId
        }
        .result
    ).map(s => s.map(mapFromDb))

  override def findVersionsUsage(versionIdx: Long): Future[Seq[Application]] = db.run {
    Tables.Application
      .filter(a => a.servicesInStage @> List(versionIdx.toString))
      .result
  }.map(_.map(mapFromDb))

  override def get(name: String): Future[Option[Application]] = {
    db.run(
      Tables.Application
        .filter(_.applicationName === name)
        .result.headOption
    ).map(s => mapFromDb(s))
  }
}

object ApplicationRepository extends CompleteJsonProtocol {

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
      namespace = dbType.namespace
    )
  }
}
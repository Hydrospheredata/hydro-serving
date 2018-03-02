package io.hydrosphere.serving.manager.repository.db

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.repository.ApplicationRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ApplicationRepositoryImpl(
  implicit val executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ApplicationRepository with Logging with CommonJsonSupport {

  import spray.json._
  import databaseService._
  import databaseService.driver.api._
  import ApplicationRepositoryImpl.mapFromDb

  private def getServices(l: ApplicationExecutionGraph): List[String] =
    l.stages.flatMap(s => s.services.map(c => c.serviceDescription.toServiceName()))

  override def create(entity: Application): Future[Application] =
    db.run(
      Tables.Application returning Tables.Application += Tables.ApplicationRow(
        id = entity.id,
        applicationName = entity.name,
        applicationContract = entity.contract.toString(),
        executionGraph = entity.executionGraph.toJson.toString(),
        servicesInStage = getServices(entity.executionGraph).map(v => v.toString),
        kafkaStreams = entity.kafkaStreaming.map(p=>p.toJson.toString())
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
      serv.kafkaStreams
    )

    db.run(query.update(
      value.name,
      value.executionGraph.toJson.toString(),
      getServices(value.executionGraph),
      value.kafkaStreaming.map(_.toJson.toString)
    ))
  }

  override def getByName(name: String): Future[Option[Application]] =
    db.run(
      Tables.Application
        .filter(_.applicationName === name)
        .result.headOption
    ).map(s => mapFromDb(s))

  override def getLockForApplications(): Future[Any] =
    Future.successful(Unit)

  override def returnLockForApplications(lockInfo: Any): Future[Unit] =
    Future.successful(Unit)

  override def getKeysNotInApplication(keysSet: Set[ServiceKeyDescription], applicationId: Long): Future[Seq[Application]] =
    db.run(
      Tables.Application
        .filter(p => {p.servicesInStage @> keysSet.map(v => v.toServiceName()).toList && p.id =!= applicationId})
        .result
    ).map(s => s.map(ss => mapFromDb(ss)))
}

object ApplicationRepositoryImpl extends CommonJsonSupport {

  import spray.json._

  def mapFromDb(dbType: Option[Tables.Application#TableElementType]): Option[Application] =
    dbType.map(r => mapFromDb(r))

  def mapFromDb(dbType: Tables.Application#TableElementType): Application = {
    Application(
      id = dbType.id,
      name = dbType.applicationName,
      executionGraph = dbType.executionGraph.parseJson.convertTo[ApplicationExecutionGraph],
      contract = ModelContract.fromAscii(dbType.applicationContract),
      kafkaStreaming = dbType.kafkaStreams.map(p=>p.parseJson.convertTo[ApplicationKafkaStream])
    )
  }
}

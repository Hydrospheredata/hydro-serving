package io.hydrosphere.serving.manager.infrastructure.db.repository

import cats._
import cats.implicits._

import cats.effect.Async
import io.hydrosphere.serving.manager.db.Tables
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableRepository, ServableStatus}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class DBServableRepository[F[_]](implicit
  F: Async[F],
  executionContext: ExecutionContext,
  databaseService: DatabaseService
) extends ServableRepository[F] with Logging {

  import DBServableRepository._
  import databaseService._
  import databaseService.driver.api._

  override def update(entity: Servable): F[Servable] = {
    AsyncUtil.futureAsync {
      val (statusText, host, port) = entity.status match {
        case ServableStatus.Running(h, p) => ("Running", Some(h), Some(p))
        case ServableStatus.Stopped => ("Stopped", None, None)
        case ServableStatus.Starting => ("Starting", None, None)
      }
  
      val row = Tables.ServableRow(
        serviceId = entity.id,
        serviceName = entity.serviceName,
        modelVersionId = entity.modelVersionId,
        statusText = statusText,
        host = host,
        port = port
      )
      db.run(Tables.Servable.insertOrUpdate(row))
    }.as(entity)
  }

  override def get(id: Long): F[Option[Servable]] = {
    AsyncUtil.futureAsync {
      db.run(Tables.Servable.filter(_.serviceId === id).result.headOption)
    }.flatMap({
      case Some(e) => F.fromEither(mapFrom(e)).map(_.some)
      case None => F.pure(None)
    })
  }

  override def delete(id: Long): F[Int] = AsyncUtil.futureAsync {
    db.run(Tables.Servable.filter(_.serviceId === id).delete)
  }

  override def all(): F[Seq[Servable]] = {
    AsyncUtil.futureAsync {
      db.run(Tables.Servable.result)
    }.map(_.map(x => mapFrom(x)).collect { case Right(v) => v })
  }

  override def getByServiceName(serviceName: String): F[Option[Servable]] = {
    AsyncUtil.futureAsync {
      db.run(Tables.Servable.filter(_.serviceName === serviceName).result.headOption)
    }.flatMap({
      case Some(e) => F.fromEither(mapFrom(e)).map(_.some)
      case None => F.pure(None)
    })
  }

  override def fetchByIds(ids: Seq[Long]): F[Seq[Servable]] = {
    AsyncUtil.futureAsync {
      if (ids.isEmpty) {
        Future.successful(Seq())
      } else {
        db.run(Tables.Servable.filter(_.serviceId inSetBind ids).result)
      }
    }.map(_.map(x => mapFrom(x)).collect { case Right(v) => v })
  }

  override def fetchServices(serviceDescs: Set[Long]): F[Seq[Servable]] = {
    AsyncUtil.futureAsync {
      db.run(Tables.Servable.filter(_.modelVersionId inSetBind serviceDescs).result)
    }.map(_.map(x => mapFrom(x)).collect { case Right(v) => v })
  }
}

object DBServableRepository {
  
  def servableStatus(text: String, host: Option[String], port: Option[Int]): Either[Throwable, ServableStatus] = {
    val hp = (host, port).tupled
    (text, hp) match {
      case ("Running", Some((h, p))) => ServableStatus.Running(h, p).asRight
      case ("Stopped", _) => ServableStatus.Stopped.asRight
      case ("Starting", _) => ServableStatus.Starting.asRight
      case (x, _) => new Exception(s"Invalid status: $x, host: $host, port: $port").asLeft
    }
  }

  def mapFrom(service: Tables.ServableRow): Either[Throwable, Servable] = {
    import service._
    
    servableStatus(statusText, host, port)
      .map(st => Servable(serviceId, service.modelVersionId, serviceName, st))
  }
}
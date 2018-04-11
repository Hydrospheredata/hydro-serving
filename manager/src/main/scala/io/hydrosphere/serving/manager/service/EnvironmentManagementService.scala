package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.model.{HFResult, Result}
import io.hydrosphere.serving.manager.repository.EnvironmentRepository
import org.apache.logging.log4j.scala.Logging
import Result.Implicits._
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.db.Environment

import scala.concurrent.{ExecutionContext, Future}

trait EnvironmentManagementService {
  def all(): Future[Seq[Environment]]

  def create(r: CreateEnvironmentRequest): Future[Environment]

  def delete(environmentId: Long): Future[Unit]

  def get(environmentId: Long): HFResult[Environment]
}

class EnvironmentManagementServiceImpl(
  environmentRepository: EnvironmentRepository
)(
  implicit ec: ExecutionContext
) extends EnvironmentManagementService with Logging {
  override def get(environmentId: Long): HFResult[Environment] = {
    environmentId match {
      case AnyEnvironment.`id` =>
        Result.okF(AnyEnvironment)
      case _ =>
        environmentRepository.get(environmentId).map(_.toHResult(ClientError(s"Can't find environment with id $environmentId")))
    }
  }

  override def all(): Future[Seq[Environment]] =
    environmentRepository.all().map(_ :+ AnyEnvironment)

  override def create(r: CreateEnvironmentRequest): Future[Environment] =
    environmentRepository.create(r.toEnvironment)

  override def delete(environmentId: Long): Future[Unit] =
    environmentRepository.delete(environmentId).map(_ => Unit)
}

object AnyEnvironment extends Environment(-1, "Without Env", Seq.empty)
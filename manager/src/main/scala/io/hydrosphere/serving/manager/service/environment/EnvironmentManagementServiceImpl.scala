package io.hydrosphere.serving.manager.service.environment

import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model.db.Environment
import io.hydrosphere.serving.manager.model.{HFResult, Result}
import io.hydrosphere.serving.manager.repository.EnvironmentRepository
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

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
        environmentRepository
          .get(environmentId)
          .map(_.toHResult(ClientError(s"Can't find environment with id $environmentId")))
    }
  }

  override def all(): Future[Seq[Environment]] =
    environmentRepository.all().map(_ :+ AnyEnvironment)

  override def create(name: String, placeholders: Seq[Any]): HFResult[Environment] = {
    environmentRepository.get(name).flatMap {
      case Some(_) => Result.clientErrorF(s"Environment '$name' already exists")
      case None =>
        val environment = Environment(
          name = name,
          placeholders = placeholders,
          id = 0
        )
        environmentRepository.create(environment).map(Result.ok)
    }
  }

  override def delete(environmentId: Long): Future[Unit] =
    environmentRepository.delete(environmentId).map(_ => Unit)
}

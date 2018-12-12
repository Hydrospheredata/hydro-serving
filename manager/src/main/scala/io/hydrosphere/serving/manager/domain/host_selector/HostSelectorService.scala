package io.hydrosphere.serving.manager.domain.host_selector

import io.hydrosphere.serving.model.api.Result.ClientError
import io.hydrosphere.serving.model.api.{HFResult, Result}
import org.apache.logging.log4j.scala.Logging
import Result.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class HostSelectorService(
  environmentRepository: HostSelectorRepositoryAlgebra[Future]
)(implicit ec: ExecutionContext) extends Logging {

  def get(environmentId: Long): HFResult[HostSelector] = {
    environmentId match {
      case AnyHostSelector.`id` =>
        Result.okF(AnyHostSelector)
      case _ =>
        environmentRepository
          .get(environmentId)
          .map(_.toHResult(ClientError(s"Can't find environment with id $environmentId")))
    }
  }

  def get(name: String): Future[Option[HostSelector]] = {
    environmentRepository.get(name)
  }

  def all(): Future[Seq[HostSelector]] =
    environmentRepository.all().map(_ :+ AnyHostSelector)

  def create(name: String, placeholder: String): HFResult[HostSelector] = {
    environmentRepository.get(name).flatMap {
      case Some(_) => Result.clientErrorF(s"Environment '$name' already exists")
      case None =>
        val environment = HostSelector(
          name = name,
          placeholder = placeholder,
          id = 0L
        )
        environmentRepository.create(environment).map(Result.ok)
    }
  }

  def delete(environmentId: Long): Future[Unit] =
    environmentRepository.delete(environmentId).map(_ => Unit)
}

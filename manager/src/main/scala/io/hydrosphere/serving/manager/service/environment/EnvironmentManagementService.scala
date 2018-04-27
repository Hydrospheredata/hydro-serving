package io.hydrosphere.serving.manager.service.environment

import io.hydrosphere.serving.manager.model.HFResult
import io.hydrosphere.serving.manager.model.db.Environment

import scala.concurrent.Future

trait EnvironmentManagementService {
  def all(): Future[Seq[Environment]]

  def create(name: String, placeholders: Seq[Any]): HFResult[Environment]

  def delete(environmentId: Long): Future[Unit]

  def get(environmentId: Long): HFResult[Environment]
}

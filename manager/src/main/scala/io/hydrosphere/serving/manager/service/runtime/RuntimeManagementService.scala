package io.hydrosphere.serving.manager.service.runtime


import io.hydrosphere.serving.manager.model.db.{CreateRuntimeRequest, PullRuntime, Runtime}
import io.hydrosphere.serving.model.api.HFResult

import scala.concurrent.Future

trait RuntimeManagementService {
  def lookupByModelType(modelType: Set[String]): Future[Seq[Runtime]]

  def lookupByTag(tag: Set[String]): Future[Seq[Runtime]]

  def all(): Future[Seq[Runtime]]

  def create(request: CreateRuntimeRequest): HFResult[PullRuntime]

  def get(id: Long): HFResult[Runtime]

  def getPullStatus(requestId: Long): HFResult[PullRuntime]
}

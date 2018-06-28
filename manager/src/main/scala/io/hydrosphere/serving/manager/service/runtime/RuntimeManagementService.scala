package io.hydrosphere.serving.manager.service.runtime

import java.util.UUID

import io.hydrosphere.serving.manager.model.{HFResult, HResult}
import io.hydrosphere.serving.manager.model.db.Runtime
import io.hydrosphere.serving.manager.service.ServiceTask

import scala.concurrent.Future

trait RuntimeManagementService {
  def lookupByModelType(modelType: Set[String]): Future[Seq[Runtime]]

  def lookupByTag(tag: Set[String]): Future[Seq[Runtime]]

  def all(): Future[Seq[Runtime]]

  def create(request: CreateRuntimeRequest): HFResult[ServiceTask[PullDocker, String]]

  def sync(request: SyncRuntimeRequest): HFResult[ServiceTask[SyncRuntimeRequest, Runtime]]

  def syncAll(): HFResult[Seq[ServiceTask[SyncRuntimeRequest, Runtime]]]

  def get(id: Long): HFResult[Runtime]

  def getCreationStatus(requestId: UUID): HResult[ServiceTask[PullDocker, String]]
}

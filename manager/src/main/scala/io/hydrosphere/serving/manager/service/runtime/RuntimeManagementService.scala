package io.hydrosphere.serving.manager.service.runtime

import java.util.UUID

import io.hydrosphere.serving.manager.model.HFResult
import io.hydrosphere.serving.manager.model.db.Runtime
import io.hydrosphere.serving.manager.service.{ServiceTask, ServiceTaskRunning}

import scala.concurrent.Future

trait RuntimeManagementService {
  def lookupByModelType(modelType: Set[String]): Future[Seq[Runtime]]

  def lookupByTag(tag: Set[String]): Future[Seq[Runtime]]

  def all(): Future[Seq[Runtime]]

  def create(request: CreateRuntimeRequest): HFResult[ServiceTaskRunning[CreateRuntimeRequest, Runtime]]

  def sync(request: SyncRuntimeRequest): HFResult[ServiceTaskRunning[SyncRuntimeRequest, Runtime]]

  def syncAll(): HFResult[Seq[ServiceTaskRunning[SyncRuntimeRequest, Runtime]]]

  def get(id: Long): HFResult[Runtime]

  def getCreationStatus(requestId: UUID): HFResult[ServiceTask[CreateRuntimeRequest, Runtime]]
}

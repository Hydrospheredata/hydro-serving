package io.hydrosphere.serving.manager.service.management.runtime

import io.hydrosphere.serving.manager.model.Runtime

import scala.concurrent.Future

trait RuntimeManagementService {
  def lookupByModelType(modelType: Set[String]): Future[Seq[Runtime]]

  def lookupByTag(tag: Set[String]): Future[Seq[Runtime]]

  def all(): Future[Seq[Runtime]]

  def create(entity: CreateRuntimeRequest): Future[Runtime]

  def get(id: Long): Future[Option[Runtime]]
}

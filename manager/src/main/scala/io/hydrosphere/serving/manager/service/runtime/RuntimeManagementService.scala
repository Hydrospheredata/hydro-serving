package io.hydrosphere.serving.manager.service.runtime

import io.hydrosphere.serving.manager.model.HFResult
import io.hydrosphere.serving.manager.model.db.Runtime

import scala.concurrent.Future

trait RuntimeManagementService {
  def lookupByModelType(modelType: Set[String]): Future[Seq[Runtime]]

  def lookupByTag(tag: Set[String]): Future[Seq[Runtime]]

  def all(): Future[Seq[Runtime]]

  def create(
    name: String,
    version: String,
    modelTypes: List[String] = List.empty,
    tags: List[String] = List.empty,
    configParams: Map[String, String] = Map.empty
  ): Future[Runtime]

  def get(id: Long): HFResult[Runtime]
}

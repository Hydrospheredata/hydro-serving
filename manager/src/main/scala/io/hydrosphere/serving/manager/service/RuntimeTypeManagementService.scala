package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.repository.RuntimeTypeRepository
import io.hydrosphere.serving.manager.model.Runtime

import scala.concurrent.Future

trait RuntimeTypeManagementService {
  def lookupRuntimeType(modelType: ModelType): Future[Seq[Runtime]]
  def lookupTag(tag: String): Future[Seq[Runtime]]
}

class RuntimeTypeManagementServiceImpl(
  runtimeTypeRepository: RuntimeTypeRepository
) extends RuntimeTypeManagementService {
  override def lookupRuntimeType(modelType: ModelType): Future[Seq[Runtime]] = {
    lookupTag(modelType.toTag)
  }

  override def lookupTag(tag: String): Future[Seq[Runtime]] = runtimeTypeRepository.fetchByTags(Seq(tag))
}

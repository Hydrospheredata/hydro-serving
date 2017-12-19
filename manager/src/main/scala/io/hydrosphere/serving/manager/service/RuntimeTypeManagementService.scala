package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.repository.RuntimeTypeRepository
import io.hydrosphere.serving.model.RuntimeType
import io.hydrosphere.serving.model_api.ModelType

import scala.concurrent.Future

trait RuntimeTypeManagementService {
  def lookupRuntimeType(modelType: ModelType): Future[Seq[RuntimeType]]
  def lookupTag(tag: String): Future[Seq[RuntimeType]]
}

class RuntimeTypeManagementServiceImpl(
  runtimeTypeRepository: RuntimeTypeRepository
) extends RuntimeTypeManagementService {
  override def lookupRuntimeType(modelType: ModelType): Future[Seq[RuntimeType]] = {
    lookupTag(modelType.toTag)
  }

  override def lookupTag(tag: String): Future[Seq[RuntimeType]] = runtimeTypeRepository.fetchByTags(Seq(tag))
}

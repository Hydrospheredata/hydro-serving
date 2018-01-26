package io.hydrosphere.serving.manager.service.management.runtime

import io.hydrosphere.serving.manager.model.Runtime
import io.hydrosphere.serving.manager.repository.RuntimeRepository
import io.hydrosphere.serving.manager.service.contract.ModelType

import scala.concurrent.Future

class RuntimeManagementServiceImpl(
  runtimeRepository: RuntimeRepository
) extends RuntimeManagementService {

  override def lookupByModelType(modelTypes: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByModelType(
      modelTypes.map(ModelType.fromTag).toSeq
    )

  override def lookupByTag(tags: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByTags(tags.toSeq)

  override def all(): Future[Seq[Runtime]] =
    runtimeRepository.all()

  override def create(entity: CreateRuntimeRequest): Future[Runtime] =
    runtimeRepository.create(entity.toRuntimeType)

  override def get(id: Long): Future[Option[Runtime]] =
    runtimeRepository.get(id)
}

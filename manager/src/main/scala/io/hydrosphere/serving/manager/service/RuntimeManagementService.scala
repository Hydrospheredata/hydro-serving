package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.repository.RuntimeRepository
import io.hydrosphere.serving.manager.model.Runtime

import scala.concurrent.Future

case class CreateRuntimeRequest(
  name: String,
  version: String,
  modelTypes: Option[List[String]] = None,
  tags: Option[List[String]] = None,
  configParams: Option[Map[String, String]] = None
) {
  def toRuntimeType: Runtime = {
    Runtime(
      id = 0,
      name = this.name,
      version = this.version,
      suitableModelType = this.modelTypes
        .getOrElse(List.empty)
        .map(ModelType.fromTag),
      tags = this.tags.getOrElse(List.empty),
      configParams = this.configParams.getOrElse(Map.empty)
    )
  }
}

trait RuntimeManagementService {
  def lookupByModelType(modelType: Set[String]): Future[Seq[Runtime]]

  def lookupByTag(tag: Set[String]): Future[Seq[Runtime]]

  def all(): Future[Seq[Runtime]]

  def create(entity: CreateRuntimeRequest): Future[Runtime]

  def get(id: Long): Future[Option[Runtime]]
}

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

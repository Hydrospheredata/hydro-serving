package io.hydrosphere.serving.manager.service.runtime

import io.hydrosphere.serving.manager.model.{HFResult, Result}
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Runtime
import io.hydrosphere.serving.manager.repository.RuntimeRepository

import scala.concurrent.{ExecutionContext, Future}

class RuntimeManagementServiceImpl(
  runtimeRepository: RuntimeRepository
)(
  implicit ex: ExecutionContext
) extends RuntimeManagementService {

  override def lookupByModelType(modelTypes: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByModelType(
      modelTypes.map(ModelType.fromTag).toSeq
    )

  override def lookupByTag(tags: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByTags(tags.toSeq)

  override def all(): Future[Seq[Runtime]] =
    runtimeRepository.all()

  override def create(
    name: String,
    version: String,
    modelTypes: List[String] = List.empty,
    tags: List[String] = List.empty,
    configParams: Map[String, String] = Map.empty
  ): HFResult[Runtime] = {
    runtimeRepository.fetchByNameAndVersion(name, version).flatMap {
      case Some(_) => Result.clientErrorF(s"Runtime $name:$version already exists")
      case None =>
        val rType = Runtime(
          id = 0,
          name = name,
          version = version,
          suitableModelType = modelTypes
            .map(ModelType.fromTag),
          tags = tags,
          configParams = configParams
        )
        runtimeRepository.create(rType).map(Result.ok)
    }
  }

  override def get(id: Long): HFResult[Runtime] =
    runtimeRepository.get(id).map(_.toHResult(ClientError(s"Can't find Runtime with id $id")))
}

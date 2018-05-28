package io.hydrosphere.serving.manager.service.model_version

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.contract.utils.ops.ModelContractOps._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.{Model, ModelVersion}
import io.hydrosphere.serving.manager.model.{HFResult, Result}
import io.hydrosphere.serving.manager.repository.ModelVersionRepository
import io.hydrosphere.serving.manager.service.contract.ContractUtilityService
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}

class ModelVersionManagementServiceImpl(
  modelVersionRepository: ModelVersionRepository,
  modelManagementService: ModelManagementService,
  contractService: ContractUtilityService
)(
  implicit executionContext: ExecutionContext
) extends ModelVersionManagementService {

  override def versionContractDescription(versionId: Long): HFResult[ContractDescription] = {
    getMap(get(versionId)) { version =>
      version.modelContract.flatten
    }
  }

  override def addModelVersion(entity: CreateModelVersionRequest): HFResult[ModelVersion] =
    entity.modelId match {
      case Some(id) =>
        modelManagementService.getModel(id).flatMap {
          case Left(a) => Future.successful(Left(a))
          case Right(b) => modelVersionRepository.create(entity.toModelVersion(b)).map(Right.apply)
        }
      case None => Result.clientErrorF("Model ID is not specified")
    }

  override def generateInputsForVersion(versionId: Long, signature: String): HFResult[JsObject] = {
    get(versionId).map { result =>
      result.right.flatMap { version =>
        contractService.generatePayload(version.modelContract, signature)
      }
    }
  }

  override def fetchLastModelVersion(modelId: Long, modelVersion: Option[Long]): HFResult[Long] = {
    modelVersion match {
      case Some(x) => modelVersionRepository.modelVersionByModelAndVersion(modelId, x).map {
        case None => Right(x)
        case _ => Result.clientError(s"$modelVersion already exists")
      }
      case None => modelVersionRepository.lastModelVersionByModel(modelId, 1).map { se =>
        Right(nextVersion(se.headOption))
      }
    }
  }

  override def lastModelVersionByModelId(id: Long, maximum: Int): Future[Seq[ModelVersion]] = {
    modelVersionRepository.lastModelVersionByModel(id: Long, maximum: Int)
  }

  override def get(key: Long): HFResult[ModelVersion] = {
    modelVersionRepository.get(key).map {
      case Some(model) => Right(model)
      case None => Result.clientError(s"Can't find a model with id: $key")
    }
  }

  override def list: Future[Seq[ModelVersion]] = modelVersionRepository.all()

  private def getMap[T, R](generator: => HFResult[T])(callback: T => R): HFResult[R] = {
    generator.map { result =>
      result.right.map(callback)
    }
  }

  private def nextVersion(lastModel: Option[ModelVersion]): Long = lastModel match {
    case None => 1
    case Some(runtime) => runtime.modelVersion + 1
  }

  override def create(version: ModelVersion): HFResult[ModelVersion] = {
    modelVersionRepository.create(version).map(Result.ok)
  }

  override def lastModelVersionForModels(ids: Seq[Long]): Future[Seq[ModelVersion]] = {
    modelVersionRepository.lastModelVersionForModels(ids)
  }

  override def modelVersionsByModelVersionIds(modelIds: Set[Long]): Future[Seq[ModelVersion]] = {
    modelVersionRepository.modelVersionsByModelVersionIds(modelIds)
  }
}

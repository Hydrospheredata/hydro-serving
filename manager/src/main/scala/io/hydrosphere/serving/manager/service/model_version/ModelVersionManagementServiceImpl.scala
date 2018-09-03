package io.hydrosphere.serving.manager.service.model_version

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.model.api.ops.ModelContractOps._
import io.hydrosphere.serving.model.api.description.ContractDescription
import io.hydrosphere.serving.manager.model.db.ModelVersion
import io.hydrosphere.serving.model.api.{HFResult, Result, TensorExampleGenerator, TensorUtil}
import io.hydrosphere.serving.manager.repository.ModelVersionRepository
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.model.api.Result.HError
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}

class ModelVersionManagementServiceImpl(
  modelVersionRepository: ModelVersionRepository,
  modelManagementService: ModelManagementService
)(
  implicit executionContext: ExecutionContext
) extends ModelVersionManagementService with Logging {

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
        TensorExampleGenerator.generatePayload(version.modelContract, signature)
      }
    }
  }

  override def fetchLastModelVersion(modelId: Long, modelVersion: Option[Long]): HFResult[Long] = {
    modelVersion match {
      case Some(x) => modelVersionRepository.modelVersionByModelAndVersion(modelId, x).map {
        case None =>
          logger.debug(s"modelId=$modelId modelVersion=$modelVersion result=$x")
          Right(x)
        case _ =>
          logger.error(s"$modelVersion already exists")
          Result.clientError(s"$modelVersion already exists")
      }
      case None => modelVersionRepository.lastModelVersionByModel(modelId, 1).map { se =>
        val result = nextVersion(se.headOption)
        logger.debug(s"modelId=$modelId modelVersion=$modelVersion result=$result")
        Right(result)
      }
    }
  }

  override def lastModelVersionByModelId(id: Long, maximum: Int): Future[Seq[ModelVersion]] = {
    modelVersionRepository.lastModelVersionByModel(id: Long, maximum: Int)
  }

  override def get(key: Long): HFResult[ModelVersion] = {
    modelVersionRepository.get(key).map {
      case Some(model) => Right(model)
      case None => Result.clientError(s"Can't find a model version with id: $key")
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

  override def listForModel(modelId: Long): HFResult[Seq[ModelVersion]] = {
    modelVersionRepository.listForModel(modelId).map(Result.ok)
  }

  override def delete(versionId: Long): HFResult[ModelVersion] = {
    val f = for {
      version <- EitherT(get(versionId))
      _ <- EitherT.liftF[Future, HError, Int](modelVersionRepository.delete(versionId))
    } yield {
      version
    }
    f.value
  }
}
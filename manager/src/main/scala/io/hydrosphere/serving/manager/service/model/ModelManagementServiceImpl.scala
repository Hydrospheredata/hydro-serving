package io.hydrosphere.serving.manager.service.model

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.contract.utils.ops.ModelContractOps._
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.{ModelMetadata, ModelType}
import io.hydrosphere.serving.manager.model.db.Model
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.service.contract.ContractUtilityService
import io.hydrosphere.serving.manager.service.source.ModelStorageService
import cats.data.EitherT
import cats.implicits._
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class ModelManagementServiceImpl(
  modelRepository: ModelRepository,
  modelVersionRepository: ModelVersionRepository,
  sourceManagementService: ModelStorageService,
  contractService: ContractUtilityService
)(
  implicit val ex: ExecutionContext
) extends ModelManagementService with Logging {

  override def allModels(): Future[Seq[Model]] =
    modelRepository.all()

  override def createModel(entity: CreateOrUpdateModelRequest): HFResult[Model] =
    modelRepository.create(entity.toModel).map(Right.apply)

  def create(model: Model): HFResult[Model] = {
    getModel(model.id).flatMap{
      case Left(_) => modelRepository.create(model).map(Right.apply)
      case Right(_) => Result.clientErrorF(s"Model id ${model.id} already exists")
    }
  }


  override def updateModel(model: Model): HFResult[Model] = {
    modelRepository.get(model.id).flatMap {
      case Some(existingModel) =>
        val newModel = model.copy(created = existingModel.created, updated = LocalDateTime.now())
        modelRepository.update(newModel).map(_ => Result.ok(newModel))
      case None => Result.clientErrorF(s"Can't find Model with id ${model.id}")
    }
  }

  override def updateModelRequest(entity: CreateOrUpdateModelRequest): HFResult[Model] = {
    entity.id match {
      case Some(modelId) =>
        modelRepository.get(modelId).flatMap {
          case Some(foundModel) =>
            val newModel = entity.toModel(foundModel)
            updateModel(newModel)
          case None => Result.clientErrorF(s"Can't find Model with id ${entity.id.get}")
        }
      case None => Result.clientErrorF("Id is required for this action")
    }
  }

  def deleteModel(modelName: String): Future[Model] = {
    modelRepository.get(modelName).flatMap {
      case Some(model) =>
        modelRepository.delete(model.id)
        Future.successful(model)
      case None =>
        Future.failed(new NoSuchElementException(s"$modelName model"))
    }
  }

  override def generateModelPayload(modelId: Long, signature: String): HFResult[JsObject] = {
    getModel(modelId).map { result =>
      result.right.flatMap { model =>
        contractService.generatePayload(model.modelContract, signature)
      }
    }
  }

  override def submitContract(modelId: Long, prototext: String): HFResult[Model] = {
    ModelContract.validateAscii(prototext) match {
      case Left(a) => Result.clientErrorF(a.msg)
      case Right(b) => updateModelContract(modelId, b)
    }
  }

  override def submitFlatContract(
    modelId: Long,
    contractDescription: ContractDescription
  ): HFResult[Model] = {
    try {
      val contract = contractDescription.toContract // TODO Error handling
      updateModelContract(modelId, contract)
    } catch {
      case _: IllegalArgumentException => Result.clientErrorF("Incorrect contract description")
    }
  }

  override def submitBinaryContract(modelId: Long, bytes: Array[Byte]): HFResult[Model] = {
    ModelContract.validate(bytes) match {
      case Failure(exception) => Result.clientErrorF(s"Incorrect model contract: ${exception.getMessage}")
      case Success(value) => updateModelContract(modelId, value)
    }
  }

  private def updateModelContract(modelId: Long, modelContract: ModelContract): HFResult[Model] = {
    getModel(modelId).flatMap {
      case Left(err) => Result.errorF(err)
      case Right(model) =>
        val newModel = model.copy(modelContract = modelContract)
        updateModel(newModel)
    }
  }

  override def modelsByType(types: Set[String]): Future[Seq[Model]] =
    modelRepository.fetchByModelType(types.map(ModelType.fromTag).toSeq)

  override def getModel(id: Long): HFResult[Model] =
    modelRepository.get(id).map {
      case Some(model) => Result.ok(model)
      case None => Result.clientError(s"Can't find a model with id: $id")
    }

  def getModel(name: String): HFResult[Model] = {
    modelRepository.get(name).map {
      case Some(model) => Result.ok(model)
      case None => Result.clientError(s"Can't find a model with name: $name")
    }
  }

  override def modelContractDescription(modelId: Long): HFResult[ContractDescription] = {
    val f = for {
      model <- EitherT(getModel(modelId))
    } yield model.modelContract.flatten
    f.value
  }

  override def uploadModel(upload: ModelUpload): HFResult[Model] = {
    val f = for {
      result <- EitherT(sourceManagementService.upload(upload))
      request = CreateOrUpdateModelRequest(
        id = None,
        name = result.name,
        modelType = result.modelType,
        description = result.description,
        modelContract = result.modelContract
      )
      r <- EitherT(upsertRequest(request))
    } yield r
    f.value
  }

  private def metadataToCreate(modelMetadata: ModelMetadata, source: String): CreateOrUpdateModelRequest = {
    CreateOrUpdateModelRequest(
      id = None,
      name = modelMetadata.modelName,
      modelType = modelMetadata.modelType,
      description = None,
      modelContract = modelMetadata.contract
    )
  }

  private def applyModelMetadata(modelMetadata: ModelMetadata, model: Model, updated: LocalDateTime = LocalDateTime.now()): Model = {
    model.copy(
      name = modelMetadata.modelName,
      modelType = modelMetadata.modelType,
      modelContract = modelMetadata.contract,
      updated = updated
    )
  }


  private def upsertRequest(request: CreateOrUpdateModelRequest): Future[Either[Result.HError, Model]] = {
    modelRepository.get(request.name).flatMap {
      case Some(model) =>
        val updateRequest = request.copy(id = Some(model.id))
        logger.info(s"Updating uploaded model with id: ${updateRequest.id} name: ${updateRequest.name}, type: ${updateRequest.modelType}")
        updateModelRequest(updateRequest)
      case None =>
        logger.info(s"Creating uploaded model with name: ${request.name}, type: ${request.modelType}")
        createModel(request)
    }
  }
}

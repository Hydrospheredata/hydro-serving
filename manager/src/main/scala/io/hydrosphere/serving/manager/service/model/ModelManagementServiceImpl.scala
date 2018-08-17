package io.hydrosphere.serving.manager.service.model

import java.time.LocalDateTime

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.model.api._
import io.hydrosphere.serving.model.api.ops.ModelContractOps._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.model.db.Model
import io.hydrosphere.serving.manager.repository.{ModelRepository, ModelVersionRepository}
import io.hydrosphere.serving.manager.service.source.ModelStorageService
import io.hydrosphere.serving.model.api.Result.HError
import io.hydrosphere.serving.model.api.{HFResult, ModelType, Result}
import io.hydrosphere.serving.model.api.description.ContractDescription
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class ModelManagementServiceImpl(
  modelRepository: ModelRepository,
  modelVersionRepository: ModelVersionRepository,
  sourceManagementService: ModelStorageService
)(
  implicit val ex: ExecutionContext
) extends ModelManagementService with Logging {

  override def allModels(): Future[Seq[Model]] =
    modelRepository.all()

  override def createModel(entity: CreateModelRequest): HFResult[Model] = {
    val now = LocalDateTime.now()
    val inputModel = Model(
      id = 0,
      name = entity.name,
      modelType = entity.modelType,
      description = entity.description,
      modelContract = entity.modelContract,
      created = now,
      updated = now
    )

    getModel(inputModel.id).flatMap {
      case Left(_) => modelRepository.create(inputModel).map(Result.ok)
      case Right(_) => Result.clientErrorF(s"Model id ${inputModel.id} already exists")
    }
  }

  override def updateModel(entity: UpdateModelRequest): HFResult[Model] = {
    val f = for {
      foundModel <- EitherT(getModel(entity.id))
      filledModel = entity.fillModel(foundModel).copy(updated = LocalDateTime.now())
      _ <- EitherT(checkIfUnique(foundModel, filledModel))
      _ <- EitherT(sourceManagementService.rename(foundModel.name, filledModel.name))
      _ <- EitherT.liftF[Future, HError, Int](modelRepository.update(filledModel))
    } yield filledModel
    f.value
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
        TensorExampleGenerator.generatePayload(model.modelContract, signature)
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

  private def updateModelContract(modelId: Long, newModelContract: ModelContract): HFResult[Model] = {
    val f = for {
      model <- EitherT(getModel(modelId))
      updateRequest = UpdateModelRequest(
        id = model.id,
        name = model.name,
        modelType = model.modelType,
        description = model.description,
        modelContract = newModelContract
      )
      updatedModel <- EitherT(updateModel(updateRequest))
    } yield updatedModel
    f.value
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
      request = CreateModelRequest(
        name = result.name,
        modelType = result.modelType,
        description = result.description,
        modelContract = result.modelContract
      )
      r <- EitherT(upsertRequest(request))
    } yield r
    f.value
  }

  private def upsertRequest(request: CreateModelRequest): HFResult[Model] = {
    modelRepository.get(request.name).flatMap {
      case Some(model) =>
        val updateRequest = UpdateModelRequest(
          id = model.id,
          name = request.name,
          modelType = request.modelType,
          description = request.description,
          modelContract = request.modelContract
        )
        logger.info(s"Updating uploaded model with id: ${updateRequest.id} name: ${updateRequest.name}, type: ${updateRequest.modelType}")
        updateModel(updateRequest)

      case None =>
        logger.info(s"Creating uploaded model with name: ${request.name}, type: ${request.modelType}")
        createModel(request)
    }
  }

  private def checkIfUnique(targetModel: Model, newModelInfo: Model): HFResult[Model] = {
    modelRepository.get(newModelInfo.name).map {
      case Some(model) if model.id == targetModel.id => // it's the same model - ok
        Result.ok(targetModel)

      case Some(model) => // it's other model - not ok
        val errMsg = s"There is already a model with same name: ${model.name}(${model.id}) -> ${newModelInfo.name}(${newModelInfo.id})"
        logger.error(errMsg)
        Result.clientError(errMsg)

      case None => // name is unique - ok
        Result.ok(targetModel)
    }
  }

  override def delete(modelId: Long): HFResult[Model] = {
    val f = for {
      model <- EitherT(getModel(modelId))
      _ <- EitherT.liftF[Future, HError, Int](modelRepository.delete(modelId))
    } yield model
    f.value
  }
}

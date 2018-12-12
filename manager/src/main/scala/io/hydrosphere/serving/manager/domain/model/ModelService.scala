package io.hydrosphere.serving.manager.domain.model

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUpload
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionService}
import io.hydrosphere.serving.manager.infrastructure.storage.ModelStorageService
import io.hydrosphere.serving.model.api.Result.HError
import io.hydrosphere.serving.model.api.{HFResult, Result}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}


class ModelService(
  modelRepository: ModelRepositoryAlgebra[Future],
  modelVersionService: ModelVersionService,
  storageService: ModelStorageService
)(implicit val ex: ExecutionContext) extends Logging {

  def allModels(): Future[Seq[Model]] =
    modelRepository.all()

  def createModel(entity: CreateModelRequest): HFResult[Model] = {
    val inputModel = Model(
      id = 0,
      name = entity.name,
    )

    getModel(inputModel.id).flatMap {
      case Left(_) => modelRepository.create(inputModel).map(Result.ok)
      case Right(_) => Result.clientErrorF(s"Model id ${inputModel.id} already exists")
    }
  }

  def updateModel(entity: UpdateModelRequest): HFResult[Model] = {
    val f = for {
      foundModel <- EitherT(getModel(entity.id))
      filledModel = entity.fillModel(foundModel)
      _ <- EitherT(checkIfUnique(foundModel, filledModel))
      _ <- EitherT(storageService.rename(foundModel.name, filledModel.name))
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

  def getModel(id: Long): HFResult[Model] =
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

  def uploadModel(upload: ModelUpload): HFResult[ModelVersion] = {
    val f = for {
      result <- EitherT(storageService.upload(upload))
      request = CreateModelRequest(result.name)
      r <- EitherT(upsertRequest(request))
      b <- EitherT(modelVersionService.build(r, upload, result))
    } yield b
    f.value
  }

  private def upsertRequest(request: CreateModelRequest): HFResult[Model] = {
    modelRepository.get(request.name).flatMap {
      case Some(model) =>
        val updateRequest = UpdateModelRequest(
          id = model.id,
          name = request.name,
        )
        logger.info(s"Updating uploaded model with id: ${updateRequest.id} name: ${updateRequest.name}")
        updateModel(updateRequest)

      case None =>
        logger.info(s"Creating uploaded model with name: ${request.name}")
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

  def delete(modelId: Long): HFResult[Model] = {
    val f = for {
      model <- EitherT(getModel(modelId))
      _ <- EitherT.liftF[Future, HError, Int](modelRepository.delete(modelId))
    } yield model
    f.value
  }
}

package io.hydrosphere.serving.manager.domain.model

import java.nio.file.Path

import akka.http.scaladsl.model.StatusCodes.ClientError
import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.application.{Application, ApplicationRepositoryAlgebra, ApplicationService}
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionService}
import io.hydrosphere.serving.manager.infrastructure.storage.{ModelStorageService, StorageUploadResult}
import io.hydrosphere.serving.model.api.Result.HError
import io.hydrosphere.serving.model.api.{HFResult, Result}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}


class ModelService(
  modelRepository: ModelRepositoryAlgebra[Future],
  modelVersionService: ModelVersionService,
  storageService: ModelStorageService,
  appRepo: ApplicationRepositoryAlgebra[Future]
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

  def deleteModel(modelId: Long): HFResult[Model] = {
    val f = for {
      model <- EitherT(getModel(modelId))
      versions <- EitherT(modelVersionService.listForModel(model.id))
      _ <- EitherT(checkIfNoApps(versions))
      _ <- EitherT(modelVersionService.deleteVersions(versions))
      _ <- EitherT(modelRepository.delete(model.id).map(Result.ok))
    } yield model
    f.value
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

  def uploadModel(filePath: Path, meta: ModelUploadMetadata): HFResult[ModelVersion] = {
    val f = for {
      result <- EitherT(storageService.upload(filePath, meta))
      _ <- EitherT.fromEither[Future](validateUpload(result))
      request = CreateModelRequest(result.name)
      r <- EitherT(upsertRequest(request))
      b <- EitherT(modelVersionService.build(r, meta, result))
    } yield b
    f.value
  }

  def validateUpload(upload: StorageUploadResult): Either[HError, Unit] = {
    if (upload.modelContract.signatures.isEmpty) {
      logger.warn("Upload error: Model without signatures. Cancelling upload.")
      Result.clientError("The model has no signatures")
    } else {
      val inputsOk = upload.modelContract.signatures.forall(_.inputs.nonEmpty)
      val outputsOk = upload.modelContract.signatures.forall(_.outputs.nonEmpty)
      if (inputsOk && outputsOk) {
        Right(())
      } else {
        Result.clientError(s"Error during signature validation. (inputsOk=$inputsOk, outputsOk=$outputsOk)")
      }
    }
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

  private def checkIfNoApps(versions: Seq[ModelVersion]): HFResult[Unit] = {
    def _checkApps(usedApps: Seq[Seq[Application]]): HFResult[Unit] = {
      val allApps = usedApps.flatten.map(_.name)
      if (allApps.isEmpty) {
        Result.okF(Unit)
      } else {
        val appNames = allApps.mkString(", ")
        Result.clientErrorF(s"Can't delete the model. It's used in [$appNames].")
      }
    }

    val f = for {
      usedApps <- EitherT.liftF[Future, HError, Seq[Seq[Application]]](Future.traverse(versions.map(_.id))(appRepo.findVersionsUsage))
      _ <- EitherT(_checkApps(usedApps))
    } yield {}

    f.value
  }
}

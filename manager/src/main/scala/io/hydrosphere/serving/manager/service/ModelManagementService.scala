package io.hydrosphere.serving.manager.service

import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.contract.utils.ops.ModelContractOps._
import io.hydrosphere.serving.manager.controller.model.UploadedEntity
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.{ModelMetadata, ModelType}
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.service.modelbuild.{ModelBuildService, ModelPushService, ProgressHandler, ProgressMessage}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.contract.utils.ops.ModelContractOps._
import io.hydrosphere.serving.manager.model.api.json.TensorJsonLens
import io.hydrosphere.serving.manager.model.db.{Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.util.TarGzUtils
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

sealed trait IndexStatus extends Product {
  def model: Model
}

case class ModelUpdated(model: Model) extends IndexStatus
case class ModelDeleted(model: Model) extends IndexStatus
case class IndexError(model: Model, error: Throwable) extends IndexStatus

case class CreateOrUpdateModelRequest(
  id: Option[Long],
  name: String,
  source: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ModelContract
) {
  def toModel: Model = {
    Model(
      id = 0,
      name = this.name,
      source = this.source,
      modelType = this.modelType,
      description = this.description,
      modelContract = this.modelContract,
      created = LocalDateTime.now(),
      updated = LocalDateTime.now()
    )
  }

  def toModel(model: Model): Model = {
    model.copy(
      name = this.name,
      source = this.source,
      modelType = this.modelType,
      description = this.description,
      modelContract = this.modelContract
    )
  }
}

trait ModelManagementService {
  def indexModels(ids: Set[Long]): Future[Seq[IndexStatus]]

  def versionContractDescription(versionId: Long): Future[Option[ContractDescription]]

  def uploadModelTarball(upload: UploadedEntity.ModelUpload): HFResult[Model]

  def modelContractDescription(modelId: Long): HFResult[ContractDescription]

  def submitBinaryContract(modelId: Long, bytes: Array[Byte]): HFResult[Model]

  def submitFlatContract(modelId: Long, contractDescription: ContractDescription): HFResult[Model]

  def submitContract(modelId: Long, prototext: String): HFResult[Model]

  def allModels(): Future[Seq[Model]]

  def getModel(id: Long): HFResult[Model]

  def updateModel(entity: CreateOrUpdateModelRequest): HFResult[Model]

  def createModel(entity: CreateOrUpdateModelRequest): HFResult[Model]

  def addModel(sourceName: String, modelPath: String): Future[Option[Model]]

  def generateModelPayload(modelId: Long, signature: String): HFResult[JsObject]

  def modelsByType(types: Set[String]): Future[Seq[Model]]
}

class ModelManagementServiceImpl(
  modelRepository: ModelRepository,
  modelVersionRepository: ModelVersionRepository,
  sourceManagementService: SourceManagementService,
  contractService: ContractUtilityService
)(
  implicit val ex: ExecutionContext
) extends ModelManagementService with Logging {

  override def allModels(): Future[Seq[Model]] =
    modelRepository.all()

  override def createModel(entity: CreateOrUpdateModelRequest): HFResult[Model] =
    modelRepository.create(entity.toModel).map(Right.apply)

  override def updateModel(entity: CreateOrUpdateModelRequest): HFResult[Model] = {
    entity.id match {
      case Some(modelId) =>
        modelRepository.get(modelId).flatMap {
          case Some(foundModel) =>
            val newModel = entity.toModel(foundModel)
            modelRepository
              .update(newModel)
              .map(_ => Right(newModel))
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
        modelRepository.update(newModel).map { _ => Result.ok(newModel) }
    }
  }

  override def modelsByType(types: Set[String]): Future[Seq[Model]] =
    modelRepository.fetchByModelType(types.map(ModelType.fromTag).toSeq)

  override def getModel(id: Long): HFResult[Model] =
    modelRepository.get(id).map {
      case Some(model) => Result.ok(model)
      case None => Result.clientError(s"Can't find a model with id: $id")
    }

  override def modelContractDescription(modelId: Long): HFResult[ContractDescription] = {
    getMap(getModel(modelId)) { model =>
      model.modelContract.flatten
    }
  }

  def writeFilesToSource(source: ModelSource, files: Map[Path, Path]): Unit = {
    files.foreach {
      case (src, dest) =>
        source.writeFile(dest.toString, src.toFile)
    }
  }

  def uploadToSource(upload: UploadedEntity.ModelUpload): HFResult[CreateOrUpdateModelRequest] = {
    val fMaybeSource = upload.source match {
      case Some(sourceName) => sourceManagementService.getSource(sourceName)
      case None => sourceManagementService.getSources.map(_.headOption.toHResult(ClientError("No sources available")))
    }
    fMaybeSource.map { result =>
      result.right.map { source =>
        val unpackDir = Files.createTempDirectory(upload.name)
        val rootDir = Paths.get(upload.name)
        val uploadedFiles = TarGzUtils.decompress(upload.tarballPath, unpackDir)
        val localFiles = uploadedFiles
          .filter(_.startsWith(unpackDir))
          .map { path =>
            val relPath = unpackDir.relativize(path)
            path -> rootDir.resolve(relPath)
          }
          .toMap

        writeFilesToSource(source, localFiles)
        CreateOrUpdateModelRequest(
          id = None,
          name = upload.name,
          source = source.sourceDef.name,
          modelType = ModelType.fromTag(upload.modelType),
          description = upload.description,
          modelContract = upload.contract
        )
      }
    }
  }

  override def uploadModelTarball(upload: UploadedEntity.ModelUpload): HFResult[Model] = {
    uploadToSource(upload).flatMap {
      case Right(request) =>
        modelRepository.get(upload.name).flatMap {
          case Some(model) =>
            val updateRequest = request.copy(id = Some(model.id), source = model.source)
            logger.info(s"Updating uploaded model with id: ${updateRequest.id} name: ${updateRequest.name}, source: ${updateRequest.source}, type: ${updateRequest.modelType} ")
            updateModel(updateRequest)
          case None =>
            val newSource = s"${request.source}:${upload.name}"
            val createRequest = request.copy(source = newSource)
            logger.info(s"Creating uploaded model with name: ${createRequest.name}, source: ${createRequest.source}, type: ${createRequest.modelType}")
            createModel(createRequest)
        }
      case Left(err) => Future.successful(Left(err))
    }
  }

  private def getMap[T, R](generator: => HFResult[T])(callback: T => R): HFResult[R] = {
    generator.map { result =>
      result.right.map(callback)
    }
  }

  override def indexModels(ids: Set[Long]): Future[Seq[IndexStatus]] = {
    modelRepository.getMany(ids).flatMap { models =>
      Future.traverse(models) { model =>
        sourceManagementService.index(model.source).flatMap {
          case Success(maybeMetadata) =>
            maybeMetadata match {
              case Some(metadata) =>
                val newModel = applyModelMetadata(metadata, model)
                modelRepository.update(newModel).map(_ => ModelUpdated(newModel))

              case None =>
                // NB delete model if no files?
                modelRepository.delete(model.id).map(_ => ModelDeleted(model))
            }
          case Failure(err) =>
            Future.successful(IndexError(model, err))
        }
      }
    }
  }

  override def addModel(sourceName: String, modelPath: String): Future[Option[Model]] = {
    sourceManagementService.getSource(sourceName).flatMap {
      case Some(source) =>
        if (source.isExist(modelPath)) {
          val metadata = ModelFetcher.fetch(source, modelPath)
          val createReq = metadataToCreate(metadata, s"$sourceName:$modelPath")
          createModel(createReq)
        } else {
          Future.successful(None)
        }
      case None =>
        Future.successful(None)
    }
  }

  private def metadataToCreate(modelMetadata: ModelMetadata, source: String): CreateOrUpdateModelRequest = {
    CreateOrUpdateModelRequest(
      id = None,
      name = modelMetadata.modelName,
      source = source,
      modelType = modelMetadata.modelType,
      description = None,
      modelContract = modelMetadata.contract
    )
  }

  private def applyModelMetadata(modelMetadata: ModelMetadata, model:Model, updated: LocalDateTime = LocalDateTime.now()): Model = {
    model.copy(
      name = modelMetadata.modelName,
      modelType = modelMetadata.modelType,
      modelContract = modelMetadata.contract,
      updated = updated
    )
  }
}
package io.hydrosphere.serving.manager.service

import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime

import akka.actor.ActorRef
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.contract.utils.ops.ModelContractOps._
import io.hydrosphere.serving.manager.controller.model.UploadedEntity
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.service.actors.RepositoryIndexActor.IgnoreModel
import io.hydrosphere.serving.manager.service.modelbuild.{ModelBuildService, ModelPushService}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.util.TarGzUtils
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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
  def uploadModelTarball(upload: UploadedEntity.ModelUpload): HFResult[Model]

  def modelContractDescription(modelId: Long): HFResult[ContractDescription]

  def submitBinaryContract(modelId: Long, bytes: Array[Byte]): HFResult[Model]

  def submitFlatContract(modelId: Long, contractDescription: ContractDescription): HFResult[Model]

  def submitContract(modelId: Long, prototext: String): HFResult[Model]

  def allModels(): Future[Seq[Model]]

  def getModel(id: Long): HFResult[Model]

  def updateModel(entity: CreateOrUpdateModelRequest): HFResult[Model]

  def createModel(entity: CreateOrUpdateModelRequest): HFResult[Model]

  def updatedInModelSource(entity: Model): Future[Unit]

  def generateModelPayload(modelId: Long, signature: String): HFResult[JsObject]

  def modelsByType(types: Set[String]): Future[Seq[Model]]
}

class ModelManagementServiceImpl(
  modelRepository: ModelRepository,
  modelVersionRepository: ModelVersionRepository,
  sourceManagementService: SourceManagementService,
  contractService: ContractUtilityService,
  repoActor: ActorRef
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

  override def updatedInModelSource(entity: Model): Future[Unit] = {
    modelRepository.fetchBySource(entity.source)
      .flatMap {
        case Nil =>
          modelRepository.create(entity).map(_ => Unit)
        case _ => modelRepository.updateLastUpdatedTime(entity.source, LocalDateTime.now())
          .map(_ => Unit)
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
      case ex: IllegalArgumentException => Result.clientErrorF("Incorrect contract description")
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
        modelRepository.update(newModel).map { _ => Right(newModel) }
    }
  }

  override def modelsByType(types: Set[String]): Future[Seq[Model]] =
    modelRepository.fetchByModelType(types.map(ModelType.fromTag).toSeq)

  override def getModel(id: Long): HFResult[Model] =
    modelRepository.get(id).map {
      case Some(model) => Right(model)
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

        repoActor ! IgnoreModel(upload.name) // Add model name to blacklist to ignore watcher events

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
}
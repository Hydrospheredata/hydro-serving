package io.hydrosphere.serving.manager.service

import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime

import akka.actor.ActorRef
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.DataGenerator
import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.manager.controller.model.UploadedEntity
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.service.actors.RepositoryIndexActor.IgnoreModel
import io.hydrosphere.serving.manager.service.modelbuild.{ModelBuildService, ModelPushService, ProgressHandler, ProgressMessage}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.contract.utils.ops.ModelContractOps._
import io.hydrosphere.serving.manager.model.api.json.TensorJsonLens
import io.hydrosphere.serving.manager.util.TarGzUtils
import org.apache.logging.log4j.scala.Logging
import Result.Implicits._
import io.hydrosphere.serving.manager.model.Result.ClientError
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

case class CreateModelVersionRequest(
  imageName: String,
  imageTag: String,
  imageSHA256: String,
  modelName: String,
  modelVersion: Long,
  source: Option[String],
  runtimeTypeId: Option[Long],
  modelContract: ModelContract,
  modelId: Option[Long],
  tags: Option[List[String]],
  configParams: Option[Map[String, String]],
  modelType: String
) {

  def toModelVersion(model: Model): ModelVersion = {
    ModelVersion(
      id = 0,
      imageName = this.imageName,
      imageTag = this.imageTag,
      imageSHA256 = this.imageSHA256,
      modelName = this.modelName,
      modelVersion = this.modelVersion,
      source = this.source,
      modelContract = this.modelContract,
      created = LocalDateTime.now(),
      model = Some(model),
      modelType = ModelType.fromTag(this.modelType)
    )
  }
}


case class AggregatedModelInfo(
  model: Model,
  lastModelBuild: Option[ModelBuild],
  lastModelVersion: Option[ModelVersion],
  nextVersion: Option[Long]
)


trait ModelManagementService {
  def uploadModelTarball(upload: UploadedEntity.ModelUpload): HFResult[Model]

  def versionContractDescription(versionId: Long): HFResult[ContractDescription]

  def modelContractDescription(modelId: Long): HFResult[ContractDescription]

  def submitBinaryContract(modelId: Long, bytes: Array[Byte]): HFResult[Model]

  def submitFlatContract(modelId: Long, contractDescription: ContractDescription): HFResult[Model]

  def submitContract(modelId: Long, prototext: String): HFResult[Model]

  def buildModel(modelId: Long, flatContract: Option[ContractDescription] = None, modelVersion: Option[Long] = None): HFResult[ModelVersion]

  def allModels(): Future[Seq[Model]]

  def getModel(id: Long): HFResult[Model]

  def getModelVersion(id: Long): HFResult[ModelVersion]

  def allModelsAggregatedInfo(): Future[Seq[AggregatedModelInfo]]

  def getModelAggregatedInfo(id: Long): HFResult[AggregatedModelInfo]

  def updateModel(entity: CreateOrUpdateModelRequest): HFResult[Model]

  def createModel(entity: CreateOrUpdateModelRequest): HFResult[Model]

  def updatedInModelSource(entity: Model): Future[Unit]

  def addModelVersion(entity: CreateModelVersionRequest): HFResult[ModelVersion]

  def allModelVersion(): Future[Seq[ModelVersion]]

  def generateModelPayload(modelId: Long, signature: String): HFResult[JsObject]

  def generateInputsForVersion(versionId: Long, signature: String): HFResult[JsObject]

  def lastModelVersionByModelId(id: Long, maximum: Int): Future[Seq[ModelVersion]]

  def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]]

  def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def modelsByType(types: Set[String]): Future[Seq[Model]]
}

object ModelManagementService {
  def nextVersion(lastModel: Option[ModelVersion]): Long = lastModel match {
    case None => 1
    case Some(runtime) => runtime.modelVersion + 1
  }
}


class ModelManagementServiceImpl(
  modelRepository: ModelRepository,
  modelVersionRepository: ModelVersionRepository,
  modelBuildRepository: ModelBuildRepository,
  modelBuildScriptRepository: ModelBuildScriptRepository,
  modelBuildService: ModelBuildService,
  modelPushService: ModelPushService,
  sourceManagementService: SourceManagementService,
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

  override def addModelVersion(entity: CreateModelVersionRequest): HFResult[ModelVersion] =
    entity.modelId match {
      case Some(id) =>
        getModel(id).flatMap {
          case Left(a) => Future.successful(Left(a))
          case Right(b) => modelVersionRepository.create(entity.toModelVersion(b)).map(Right.apply)
        }
      case None => Result.clientErrorF("Model ID is not specified")
    }

  override def allModelVersion(): Future[Seq[ModelVersion]] =
    modelVersionRepository.all()

  override def lastModelVersionByModelId(id: Long, maximum: Int): Future[Seq[ModelVersion]] =
    modelVersionRepository.lastModelVersionByModel(id: Long, maximum: Int)

  override def updatedInModelSource(entity: Model): Future[Unit] = {
    modelRepository.fetchBySource(entity.source)
      .flatMap {
        case Nil =>
          addModel(entity).map(_ => Unit)
        case _ => modelRepository.updateLastUpdatedTime(entity.source, LocalDateTime.now())
          .map(_ => Unit)
      }

  }

  override def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]] =
    modelBuildRepository.listByModelId(id)

  override def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]] =
    modelBuildRepository.lastByModelId(id, maximum)

  private def buildNewModelVersion(model: Model, modelVersion: Option[Long]): HFResult[ModelVersion] = {
    fetchLastModelVersion(model.id, modelVersion).flatMap {
      case Right(version) =>
        val build = ModelBuild(
          id = 0,
          model = model,
          version = version,
          started = LocalDateTime.now(),
          finished = None,
          status = ModelBuildStatus.STARTED,
          statusText = None,
          logsUrl = None,
          modelVersion = None
        )
        fetchScriptForModel(model).flatMap { script =>
          modelBuildRepository.create(build).flatMap { modelBuild =>
            buildModelRuntime(modelBuild, script).transform(
              {
                case Right(runtime) =>
                  modelBuildRepository.finishBuild(modelBuild.id, ModelBuildStatus.FINISHED, "OK", LocalDateTime.now(), Some(runtime))
                  Right(runtime)
                case Left(err) => Result.error(err)
              },
              { ex =>
                logger.error(ex.getMessage, ex)
                modelBuildRepository.finishBuild(modelBuild.id, ModelBuildStatus.ERROR, ex.getMessage, LocalDateTime.now(), None)
                ex
              })
          }
        }
      case Left(err) => Result.errorF(err)
    }
  }

  override def buildModel(modelId: Long, flatContract: Option[ContractDescription], modelVersion: Option[Long]): HFResult[ModelVersion] = {
    getModel(modelId).flatMap {
      case Left(err) => Result.errorF(err)
      case Right(model) =>
        val newModel = flatContract match {
          case Some(newContract) =>
            submitFlatContract(model.id, newContract)
          case None => Result.okF(model)
        }
        newModel.flatMap {
          case Left(err) => Result.errorF(err)
          case Right(rModel) => buildNewModelVersion(rModel, modelVersion)
        }
    }
  }

  private def fetchScriptForModel(model: Model): Future[String] =
    modelBuildScriptRepository.get(model.modelType.toTag).flatMap {
      case Some(script) => Future.successful(script.script)
      case None => Future.successful(
        """FROM busybox:1.28.0
           LABEL MODEL_TYPE={MODEL_TYPE}
           LABEL MODEL_NAME={MODEL_NAME}
           LABEL MODEL_VERSION={MODEL_VERSION}
           VOLUME /model
           ADD {MODEL_PATH} /model""")
    }

  private def buildModelRuntime(modelBuild: ModelBuild, script: String): HFResult[ModelVersion] = {
    val handler = new ProgressHandler {
      override def handle(progressMessage: ProgressMessage): Unit =
        logger.info(progressMessage)
    }

    val imageName = modelPushService.getImageName(modelBuild)
    modelBuildService.build(modelBuild, imageName, script, handler).flatMap {
      case Left(err) => Result.errorF(err)
      case Right(sha256) =>
        val version = ModelVersion(
          id = 0,
          imageName = imageName,
          imageTag = modelBuild.version.toString,
          imageSHA256 = sha256,
          modelName = modelBuild.model.name,
          modelVersion = modelBuild.version,
          source = Some(modelBuild.model.source),
          modelContract = modelBuild.model.modelContract,
          created = LocalDateTime.now,
          model = Some(modelBuild.model),
          modelType = modelBuild.model.modelType
        )
        modelVersionRepository.create(version).map { modelVersion =>
          modelPushService.push(modelVersion, handler)
          Right(modelVersion)
        }
    }
  }

  private def getNextVersion(model: Model, modelVersion: Option[ModelVersion]): Option[Long] = {
    modelVersion match {
      case Some(version) =>
        if (model.updated.isAfter(version.created)) {
          Some(version.modelVersion + 1)
        } else {
          None
        }
      case None =>
        Some(1L)
    }
  }

  private def fetchLastModelVersion(modelId: Long, modelVersion: Option[Long]): HFResult[Long] = {
    modelVersion match {
      case Some(x) => modelVersionRepository.modelVersionByModelAndVersion(modelId, x).map {
        case None => Right(x)
        case _ => Result.clientError(s"$modelVersion already exists")
      }
      case None => modelVersionRepository.lastModelVersionByModel(modelId, 1).map { se =>
        Right(ModelManagementService.nextVersion(se.headOption))
      }
    }
  }

  private def addModel(model: Model): Future[Model] = {
    modelRepository.create(model)
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
        generatePayload(model.modelContract, signature)
      }
    }
  }

  private def generatePayload(contract: ModelContract, signature: String): HResult[JsObject] = {
    DataGenerator.forContract(contract, signature) match {
      case Some(generator) => Result.ok(TensorJsonLens.mapToJson(generator.generateInputs))
      case None => Result.clientError(s"Can't find '$signature' signature in contract")
    }
  }


  override def generateInputsForVersion(versionId: Long, signature: String): HFResult[JsObject] = {
    getModelVersion(versionId).map { result =>
      result.right.flatMap { version =>
        generatePayload(version.modelContract, signature)
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

  override def getModelVersion(id: Long): HFResult[ModelVersion] =
    modelVersionRepository.get(id).map {
      case Some(model) => Right(model)
      case None => Result.clientError(s"Can't find a model with id: $id")
    }

  def aggregatedInfo(models: Model*): Future[Seq[AggregatedModelInfo]] = {
    val ids = models.map(_.id)
    for {
      builds <- modelBuildRepository.lastForModels(ids)
      buildsMap = builds.groupBy(_.model.id)
      versions <- modelVersionRepository.lastModelVersionForModels(ids)
      versionsMap = versions.groupBy(_.model.get.id)
    } yield {
      models.map { model =>
        val lastVersion = versionsMap.get(model.id).map(_.maxBy(_.modelVersion))
        val lastBuild = buildsMap.get(model.id).map(_.maxBy(_.version))
        AggregatedModelInfo(
          model = model,
          lastModelBuild = lastBuild,
          lastModelVersion = lastVersion,
          nextVersion = getNextVersion(model, lastVersion)
        )
      }
    }
  }

  override def allModelsAggregatedInfo(): Future[Seq[AggregatedModelInfo]] = {
    modelRepository.all().flatMap(aggregatedInfo)
  }

  override def getModelAggregatedInfo(id: Long): HFResult[AggregatedModelInfo] = {
    getModel(id).flatMap {
      case Left(err) => Result.errorF(err)
      case Right(model) =>
        aggregatedInfo(model).map(_.headOption).map{
          case Some(info) => Result.ok(info)
          case None => Result.clientError(s"Can't find aggregated info for model $id")
        }
    }
  }

  override def modelContractDescription(modelId: Long): HFResult[ContractDescription] = {
    getMap(getModel(modelId)) { model =>
      model.modelContract.flatten
    }
  }

  override def versionContractDescription(versionId: Long): HFResult[ContractDescription] = {
    getMap(getModelVersion(versionId)) { version =>
      version.modelContract.flatten
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
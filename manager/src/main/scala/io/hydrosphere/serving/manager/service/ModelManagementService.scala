package io.hydrosphere.serving.manager.service

import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.DataGenerator
import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.manager.controller.model.UploadedEntity
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

  def toModelVersion(model: Option[Model]): ModelVersion = {
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
      model = model,
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
  def indexModels(ids: Set[Long]): Future[Seq[IndexStatus]]

  def uploadModelTarball(upload: UploadedEntity.ModelUpload): Future[Option[Model]]

  def versionContractDescription(versionId: Long): Future[Option[ContractDescription]]

  def modelContractDescription(modelId: Long): Future[Option[ContractDescription]]

  def submitBinaryContract(modelId: Long, bytes: Array[Byte]): Future[Option[Model]]

  def submitFlatContract(modelId: Long, contractDescription: ContractDescription): Future[Option[Model]]

  def submitContract(modelId: Long, prototext: String): Future[Option[Model]]

  def buildModel(modelId: Long, flatContract: Option[ContractDescription] = None, modelVersion: Option[Long] = None): Future[ModelVersion]

  def allModels(): Future[Seq[Model]]

  def getModel(id: Long): Future[Option[Model]]

  def allModelsAggregatedInfo(): Future[Seq[AggregatedModelInfo]]

  def getModelAggregatedInfo(id: Long): Future[Option[AggregatedModelInfo]]

  def updateModel(entity: CreateOrUpdateModelRequest): Future[Option[Model]]

  def createModel(entity: CreateOrUpdateModelRequest): Future[Option[Model]]

  def addModel(sourceName: String, modelPath: String): Future[Option[Model]]

  def addModelVersion(entity: CreateModelVersionRequest): Future[ModelVersion]

  def allModelVersion(): Future[Seq[ModelVersion]]

  def generateModelPayload(modelId: Long, signature: String): Future[Seq[JsObject]]

  def generateInputsForVersion(versionId: Long, signature: String): Future[Seq[JsObject]]

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
  sourceManagementService: SourceManagementService
)(
  implicit val ex: ExecutionContext
) extends ModelManagementService with Logging {
  override def allModels(): Future[Seq[Model]] =
    modelRepository.all()

  override def createModel(entity: CreateOrUpdateModelRequest): Future[Option[Model]] = {
    modelRepository.get(entity.name).flatMap {
      case Some(_) => Future.successful(None)
      case None => modelRepository.create(entity.toModel).map(Some(_))
    }
  }

  override def updateModel(entity: CreateOrUpdateModelRequest): Future[Option[Model]] = {
    entity.id match {
      case Some(modelId) =>
        modelRepository.get(modelId).flatMap {
          case Some(foundModel) =>
            val newModel = entity.toModel(foundModel)
            modelRepository
              .update(newModel)
              .map(_ => Some(newModel))
          case None => Future.successful(None)
        }
      case None => throw new IllegalArgumentException("Id required for this action")
    }
  }

  override def addModelVersion(entity: CreateModelVersionRequest): Future[ModelVersion] =
    fetchModel(entity.modelId).flatMap { model =>
      modelVersionRepository.create(entity.toModelVersion(model))
    }

  override def allModelVersion(): Future[Seq[ModelVersion]] =
    modelVersionRepository.all()

  override def lastModelVersionByModelId(id: Long, maximum: Int): Future[Seq[ModelVersion]] =
    modelVersionRepository.lastModelVersionByModel(id: Long, maximum: Int)

  override def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]] =
    modelBuildRepository.listByModelId(id)

  override def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]] =
    modelBuildRepository.lastByModelId(id, maximum)

  override def buildModel(modelId: Long, flatContract: Option[ContractDescription], modelVersion: Option[Long]): Future[ModelVersion] =
    modelRepository.get(modelId)
      .flatMap {
        case None => throw new IllegalArgumentException(s"Can't find Model with id $modelId")
        case Some(model) =>
          val modelFuture = flatContract match {
            case Some(contract) =>
              submitFlatContract(modelId, contract).map(m =>
                m.getOrElse(throw new IllegalArgumentException(s"Can't find Model with id $modelId")))
            case _ => Future.successful(model)
          }

          modelFuture.flatMap { m =>
            buildNewModelVersion(m, modelVersion)
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

  override def generateModelPayload(modelId: Long, signature: String): Future[Seq[JsObject]] =
    modelRepository.get(modelId).map {
      case None => throw new IllegalArgumentException(s"Can't find model modelId=$modelId")
      case Some(model) =>
        generatePayload(model.modelContract, signature)
    }

  override def generateInputsForVersion(versionId: Long, signature: String): Future[Seq[JsObject]] =
    modelVersionRepository.get(versionId).map {
      case None => throw new IllegalArgumentException(s"Can't find model version id=$versionId")
      case Some(version) =>
        generatePayload(version.modelContract, signature)
    }

  override def submitContract(modelId: Long, prototext: String): Future[Option[Model]] = {
    ModelContract.validateAscii(prototext) match {
      case Left(a) => Future.failed(new IllegalArgumentException(a.msg))
      case Right(b) => updateModelContract(modelId, b)
    }
  }

  override def submitFlatContract(
    modelId: Long,
    contractDescription: ContractDescription
  ): Future[Option[Model]] = {
    val contract = contractDescription.toContract // TODO Error handling
    updateModelContract(modelId, contract)
  }

  override def submitBinaryContract(modelId: Long, bytes: Array[Byte]): Future[Option[Model]] = {
    ModelContract.validate(bytes) match {
      case Failure(exception) => Future.failed(exception)
      case Success(value) => updateModelContract(modelId, value)
    }
  }

  override def modelsByType(types: Set[String]): Future[Seq[Model]] =
    modelRepository.fetchByModelType(types.map(ModelType.fromTag).toSeq)

  override def getModel(id: Long): Future[Option[Model]] =
    modelRepository.get(id)

  override def allModelsAggregatedInfo(): Future[Seq[AggregatedModelInfo]] = {
    modelRepository.all().flatMap(aggregatedInfo)
  }

  override def getModelAggregatedInfo(id: Long): Future[Option[AggregatedModelInfo]] = {
    modelRepository.get(id).flatMap {
      case None => Future.successful(None)
      case Some(model) => aggregatedInfo(model).map(_.headOption)
    }
  }

  override def modelContractDescription(modelId: Long): Future[Option[ContractDescription]] = {
    modelRepository.get(modelId).map { maybeModel =>
      maybeModel.map { model =>
        model.modelContract.flatten
      }
    }
  }

  override def versionContractDescription(versionId: Long): Future[Option[ContractDescription]] = {
    modelVersionRepository.get(versionId).map { maybeVersion =>
      maybeVersion.map { version =>
        version.modelContract.flatten
      }
    }
  }

  def writeFilesToSource(source: ModelSource, files: Map[Path, Path]): Unit = {
    files.foreach {
      case (src, dest) =>
        source.writeFile(dest.toString, src.toFile)
    }
  }

  def uploadToSource(upload: UploadedEntity.ModelUpload): Future[Option[CreateOrUpdateModelRequest]] = {
    val fMaybeSource = upload.source match {
      case Some(sourceName) => sourceManagementService.getSource(sourceName)
      case None => sourceManagementService.getSources.map(_.headOption)
    }
    fMaybeSource.map { maybeSource =>
      maybeSource.map { source =>
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


  override def uploadModelTarball(upload: UploadedEntity.ModelUpload): Future[Option[Model]] = {
    uploadToSource(upload).flatMap {
      case Some(request) =>
        val res = modelRepository.get(upload.name).flatMap {
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
        res
      case None => Future.successful(None)
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

  private def updateModelContract(modelId: Long, modelContract: ModelContract): Future[Option[Model]] = {
    modelRepository.get(modelId).flatMap {
      case Some(model) =>
        val newModel = model.copy(modelContract = modelContract) // TODO contract validation (?)
        modelRepository.update(newModel).map { _ => Some(newModel) }
      case None => Future.successful(None)
    }
  }

  private def generatePayload(contract: ModelContract, signature: String): Seq[JsObject] = {
    val res = DataGenerator.forContract(contract, signature)
      .getOrElse(throw new IllegalArgumentException(s"Can't find signature model signature=$signature"))
    Seq(TensorJsonLens.mapToJson(res.generateInputs))
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

  private def buildModelRuntime(modelBuild: ModelBuild, script: String): Future[ModelVersion] = {
    val handler = new ProgressHandler {
      override def handle(progressMessage: ProgressMessage): Unit =
        logger.info(progressMessage)
    }

    val imageName = modelPushService.getImageName(modelBuild)
    modelBuildService.build(modelBuild, imageName, script, handler).flatMap { sha256 =>
      modelVersionRepository.create(ModelVersion(
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
      )).flatMap { modelRuntime =>
        Future(modelPushService.push(modelRuntime, handler)).map(_ => modelRuntime)
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

  private def fetchLastModelVersion(modelId: Long, modelVersion: Option[Long]): Future[Long] = {
    modelVersion match {
      case Some(x) => modelVersionRepository.modelVersionByModelAndVersion(modelId, x).map {
        case None => x
        case _ => throw new IllegalArgumentException("You already have such version")
      }
      case _ => modelVersionRepository.lastModelVersionByModel(modelId, 1)
        .map(se => ModelManagementService.nextVersion(se.headOption))
    }
  }

  private def fetchModel(id: Option[Long]): Future[Option[Model]] = {
    if (id.isEmpty) {
      Future.successful(None)
    } else {
      modelRepository
        .get(id.get)
        .map {
          _.orElse(throw new IllegalArgumentException(s"Can't find Model with id ${id.get}"))
        }
    }
  }

  private def aggregatedInfo(models: Model*): Future[Seq[AggregatedModelInfo]] = {
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

  private def buildNewModelVersion(model: Model, modelVersion: Option[Long]): Future[ModelVersion] = {
    fetchLastModelVersion(model.id, modelVersion).flatMap { version =>
      fetchScriptForModel(model).flatMap { script =>
        modelBuildRepository.create(
          ModelBuild(
            id = 0,
            model = model,
            version = version,
            started = LocalDateTime.now(),
            finished = None,
            status = ModelBuildStatus.STARTED,
            statusText = None,
            logsUrl = None,
            modelVersion = None
          )).flatMap { modelBuild =>
          buildModelRuntime(modelBuild, script).transform(
            runtime => {
              modelBuildRepository.finishBuild(modelBuild.id, ModelBuildStatus.FINISHED, "OK", LocalDateTime.now(), Some(runtime))
              runtime
            },
            ex => {
              logger.error(ex.getMessage, ex)
              modelBuildRepository.finishBuild(modelBuild.id, ModelBuildStatus.ERROR, ex.getMessage, LocalDateTime.now(), None)
              ex
            }
          )
        }
      }
    }
  }
}

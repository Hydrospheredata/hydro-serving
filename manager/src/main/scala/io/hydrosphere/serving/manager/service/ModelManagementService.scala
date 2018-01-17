package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.api.{ContractOps, DataGenerator, ModelType}
import io.hydrosphere.serving.manager.service.modelbuild.{ModelBuildService, ModelPushService, ProgressHandler, ProgressMessage}
import io.hydrosphere.serving.manager.repository._
import spray.json.JsObject
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.model._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class CreateRuntimeTypeRequest(
  name: String,
  version: String,
  modelType: String,
  tags: Option[List[String]],
  configParams: Option[Map[String, String]]
) {
  def toRuntimeType: Runtime = {
    Runtime(
      id = 0,
      name = this.name,
      version = this.version,
      modelType = ModelType.fromTag(modelType),
      tags = this.tags.getOrElse(List()),
      configParams = this.configParams.getOrElse(Map())
    )
  }
}

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

case class CreateModelRuntime(
  imageName: String,
  imageTag: String,
  imageMD5Tag: String,
  modelName: String,
  modelVersion: Long,
  source: Option[String],
  runtimeTypeId: Option[Long],
  modelContract: ModelContract,
  modelId: Option[Long],
  tags: Option[List[String]],
  configParams: Option[Map[String, String]]
) {
  def toModelRuntime(runtimeType: Option[Runtime]): ModelVersion = {
    ModelVersion(
      id = 0,
      imageName = this.imageName,
      imageTag = this.imageTag,
      imageMD5Tag = this.imageMD5Tag,
      modelName = this.modelName,
      modelVersion = this.modelVersion,
      source = this.source,
      runtimeType = runtimeType,
      modelContract = this.modelContract,
      created = LocalDateTime.now(),
      modelId = this.modelId,
      tags = runtimeType.map(r => r.tags).getOrElse(this.tags.getOrElse(List())),
      configParams = runtimeType.map(r => r.configParams).getOrElse(this.configParams.getOrElse(Map()))
    )
  }
}

case class UpdateModelRuntime(
  modelName: String,
  modelVersion: String,
  source: Option[String],
  runtimeTypeId: Option[Long],
  outputFields: Option[List[String]],
  inputFields: Option[List[String]],
  modelId: Option[Long]
)

//TODO split service
trait ModelManagementService {
  def submitBinaryContract(modelId: Long, bytes: Array[Byte]): Future[Option[Model]]

  def submitFlatContract(modelId: Long, contractDescription: ContractOps.ContractDescription): Future[Option[Model]]

  def submitContract(modelId: Long, prototext: String): Future[Option[Model]]

  def createRuntimeType(entity: CreateRuntimeTypeRequest): Future[Runtime]

  def buildModel(modelId: Long, modelVersion: Option[Long], runtimeTypeId: Long): Future[ModelVersion]

  def buildModel(modelName: String, modelVersion: Option[Long], runtimeTypeId: Long): Future[ModelVersion]

  def allRuntimeTypes(): Future[Seq[Runtime]]

  def runtimeTypesByTag(tags: Seq[String]): Future[Seq[Runtime]]

  def allModels(): Future[Seq[Model]]

  def updateModel(entity: CreateOrUpdateModelRequest): Future[Model]

  def updateModel(modelName: String, modelSource: ModelSource): Future[Option[Model]]

  def createModel(entity: CreateOrUpdateModelRequest): Future[Model]

  def deleteModel(modelName: String): Future[Model]

  def updatedInModelSource(entity: Model): Future[Unit]

  def addModelRuntime(entity: CreateModelRuntime): Future[ModelVersion]

  def allModelRuntime(): Future[Seq[ModelVersion]]

  def modelRuntimeByTag(tags: Seq[String]): Future[Seq[ModelVersion]]

  def lastModelRuntimeByModel(id: Long, maximum: Int): Future[Seq[ModelVersion]]

  def allModelBuilds(): Future[Seq[ModelBuild]]

  def modelBuildsByModel(id: Long): Future[Seq[ModelBuild]]

  def lastModelBuildsByModel(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def createFileForModel(source: ModelSource, path: String, hash: String, createdAt: LocalDateTime, updatedAt: LocalDateTime): Future[Int]

  def updateOrCreateModelFile(source: ModelSource, modelName: String, hash: String, createdAt: LocalDateTime, updatedAt: LocalDateTime): Future[Int]

  def deleteModelFile(fileName: String): Future[Int]

  def generateModelPayload(modelName: String, signature: String): Future[Seq[JsObject]]

  def generateInputsForRuntime(runtimeId: Long, signature: String): Future[Option[Seq[JsObject]]]
}

object ModelManagementService {
  def nextVersion(lastRuntime: Option[ModelVersion]): Long = lastRuntime match {
    case None => 1
    case Some(runtime) => runtime.modelVersion + 1
  }
}


class ModelManagementServiceImpl(
  runtimeTypeRepository: RuntimeTypeRepository,
  modelRepository: ModelRepository,
  modelFilesRepository: ModelFilesRepository,
  modelRuntimeRepository: ModelRuntimeRepository,
  modelBuildRepository: ModelBuildRepository,
  runtimeTypeBuildScriptRepository: RuntimeTypeBuildScriptRepository,
  modelBuildService: ModelBuildService,
  modelPushService: ModelPushService
)(
  implicit val ex: ExecutionContext
) extends ModelManagementService with Logging {

  override def createRuntimeType(entity: CreateRuntimeTypeRequest): Future[Runtime] =
    runtimeTypeRepository.create(entity.toRuntimeType)

  override def allRuntimeTypes(): Future[Seq[Runtime]] = runtimeTypeRepository.all()

  override def allModels(): Future[Seq[Model]] = modelRepository.all()

  override def createModel(entity: CreateOrUpdateModelRequest): Future[Model] = {
    modelRepository.create(entity.toModel)
  }

  override def updateModel(entity: CreateOrUpdateModelRequest): Future[Model] = {
    entity.id match {
      case Some(modelId) =>
        modelRepository.get(modelId).flatMap{
          case Some(foundModel) =>
            val newModel = entity.toModel(foundModel)
            modelRepository
              .update(newModel)
              .map(_ => newModel)
          case None => throw new IllegalArgumentException(s"Can't find Model with id ${entity.id.get}")
        }
      case None => throw new IllegalArgumentException("Id required for this action")
    }
  }

  override def addModelRuntime(entity: CreateModelRuntime): Future[ModelVersion] =
    fetchRuntimeType(entity.runtimeTypeId).flatMap(runtimeType => {
      modelRuntimeRepository.create(entity.toModelRuntime(runtimeType))
    })

  override def allModelRuntime(): Future[Seq[ModelVersion]] = modelRuntimeRepository.all()

  override def lastModelRuntimeByModel(id: Long, maximum: Int): Future[Seq[ModelVersion]] =
    modelRuntimeRepository.lastModelRuntimeByModel(id: Long, maximum: Int)

  override def updatedInModelSource(entity: Model): Future[Unit] = {
    modelRepository.fetchBySource(entity.source)
      .flatMap {
        case Nil =>
          addModel(entity).map(p => Unit)
        case _ => modelRepository.updateLastUpdatedTime(entity.source, LocalDateTime.now()).map(p => Unit)
      }

  }

  override def allModelBuilds(): Future[Seq[ModelBuild]] =
    modelBuildRepository.all()

  override def modelBuildsByModel(id: Long): Future[Seq[ModelBuild]] =
    modelBuildRepository.listByModelId(id)

  override def lastModelBuildsByModel(id: Long, maximum: Int): Future[Seq[ModelBuild]] =
    modelBuildRepository.lastByModelId(id, maximum)

  private def buildNewModelVersion(model: Model, modelVersion: Option[Long], runtimeType: Option[Runtime]): Future[ModelVersion] = {
    fetchLastModelVersion(model.id, modelVersion).flatMap { version =>
      fetchScriptForRuntime(runtimeType).flatMap { script =>
        modelBuildRepository.create(
          ModelBuild(
            id = 0,
            model = model,
            modelVersion = version,
            started = LocalDateTime.now(),
            finished = None,
            status = ModelBuildStatus.STARTED,
            statusText = None,
            logsUrl = None,
            modelRuntime = None,
            runtimeType = runtimeType
          )).flatMap { modelBuid =>
          buildModelRuntime(modelBuid, script).transform(
            runtime => {
              modelBuildRepository.finishBuild(modelBuid.id, ModelBuildStatus.FINISHED, "OK", LocalDateTime.now(), Some(runtime))
              runtime
            },
            ex => {
              logger.error(ex.getMessage, ex)
              modelBuildRepository.finishBuild(modelBuid.id, ModelBuildStatus.ERROR, ex.getMessage, LocalDateTime.now(), None)
              ex
            }
          )
        }
      }
    }
  }

  override def buildModel(modelId: Long, modelVersion: Option[Long], runtimeTypeId: Long): Future[ModelVersion] =
    modelRepository.get(modelId)
      .flatMap {
        case None => throw new IllegalArgumentException(s"Can't find Model with id $modelId")
        case Some(model) =>
          runtimeTypeRepository.get(runtimeTypeId).flatMap{ rtype =>
            buildNewModelVersion(model, modelVersion, rtype)
          }
      }

  private def fetchScriptForRuntime(runtimeType: Option[Runtime]): Future[String] = {
    runtimeType match {
      case None => throw new IllegalArgumentException("Specify RuntimeType")
      case Some(x) => runtimeTypeBuildScriptRepository.get(x.name, Some(x.version)).flatMap({
        case Some(script) => Future.successful(script.script)
        case None => runtimeTypeBuildScriptRepository.get(x.name, None).map({
          case Some(script) => script.script
          case None => """FROM {RUNTIME_IMAGE}:{RUNTIME_VERSION}
                         LABEL MODEL_NAME={MODEL_NAME}
                         LABEL MODEL_VERSION={MODEL_VERSION}
                         ADD {MODEL_PATH} /model"""
        })
      })
    }
  }

  private def buildModelRuntime(modelBuild: ModelBuild, script: String): Future[ModelVersion] = {
    val handler = new ProgressHandler {
      override def handle(progressMessage: ProgressMessage): Unit =
        logger.info(progressMessage)
    }

    val imageName = modelPushService.getImageName(modelBuild)
    modelBuildService.build(modelBuild, imageName, script, handler).flatMap { md5 =>
      modelRuntimeRepository.create(ModelVersion(
        id = 0,
        imageName = imageName,
        imageTag = modelBuild.modelVersion.toString,
        imageMD5Tag = md5,
        modelName = modelBuild.model.name,
        modelVersion = modelBuild.modelVersion,
        source = Some(modelBuild.model.source),
        runtimeType = modelBuild.modelRuntime.flatMap(_.runtimeType),
        modelContract = modelBuild.model.modelContract,
        created = LocalDateTime.now,
        modelId = Some(modelBuild.model.id),
        tags = modelBuild.modelRuntime.flatMap(_.runtimeType).map(_.tags).getOrElse(List()),
        configParams = modelBuild.modelRuntime.flatMap(_.runtimeType).map(_.configParams).getOrElse(Map())
      )).flatMap { modelRuntime =>
        Future(modelPushService.push(modelRuntime, handler)).map(_ => modelRuntime)
      }
    }
  }

  private def fetchLastModelVersion(modelId: Long, modelVersion: Option[Long]): Future[Long] = {
    modelVersion match {
      case Some(x) => modelRuntimeRepository.modelRuntimeByModelAndVersion(modelId, x).map {
        case None => x
        case _ => throw new IllegalArgumentException("You already have such version")
      }
      case _ => modelRuntimeRepository.lastModelRuntimeByModel(modelId, 1)
        .map(se => ModelManagementService.nextVersion(se.headOption))
    }
  }

  private def fetchRuntimeType(id: Option[Long]): Future[Option[Runtime]] = {
    if (id.isEmpty) {
      Future.successful(None)
    } else {
      runtimeTypeRepository
        .get(id.get)
        .map {
          _.orElse(throw new IllegalArgumentException(s"Can't find RuntimeType with id ${id.get}"))
        }
    }
  }

  private def addModel(model: Model): Future[Model] = {
    modelRepository.create(model)
  }

  def deleteModel(modelName: String): Future[Model] = {
    modelRepository.get(modelName).flatMap {
      case Some(model) =>
        modelFilesRepository.deleteModelFiles(model.id)
        modelRepository.delete(model.id)
        Future.successful(model)
      case None =>
        Future.failed(new NoSuchElementException(s"$modelName model"))
    }
  }

  def createFileForModel(source: ModelSource, fileName: String, hash: String, createdAt: LocalDateTime, updatedAt: LocalDateTime): Future[Int] = {
    val modelName = fileName.split("/").head
    println(s"Creating file $fileName for model $modelName ...")
    updateModel(modelName, source).map { model =>
      model.foreach { m =>
        modelFilesRepository.create(
          ModelFile(-1, fileName, m, hash, createdAt, updatedAt)
        )
        println(s"Model $modelName updated")
      }
      1 // FIXME
    }
  }

  def updateOrCreateModelFile(source: ModelSource, fileName: String, hash: String, createdAt: LocalDateTime, updatedAt: LocalDateTime): Future[Int] = {
    modelFilesRepository.get(fileName).flatMap {
      case Some(modelFile) =>
        println(s"File $fileName found. Updating...")
        val newFile = ModelFile(
          modelFile.id,
          modelFile.path,
          modelFile.model,
          modelFile.hashSum,
          modelFile.createdAt,
          modelFile.updatedAt
        )
        modelFilesRepository.update(newFile).flatMap(_ =>
          modelRepository.updateLastUpdatedTime(modelFile.model.id, LocalDateTime.now()))

      case None =>
        createFileForModel(source, fileName, hash, updatedAt, updatedAt)
    }
  }

  def deleteModelFile(fileName: String): Future[Int] = {
    modelFilesRepository.get(fileName).flatMap {
      case Some(modelFile) =>
        val modelId = modelFile.model.id
        println(s"Deleting file $fileName...")
        modelFilesRepository
          .delete(modelFile.id)
          .zip(modelFilesRepository.modelFiles(modelId))
          .map {
            case (i, Nil) =>
              println(s"$fileName is the last file. Deleting model $modelId...")
              modelRepository.delete(modelId)
              i
            case (i, _) =>
              println(s"$fileName is not the last file...")
              i
          }
      case None =>
        logger.error(s"No such file: $fileName")
        Future.failed(new NoSuchElementException())
    }
  }

  override def buildModel(modelName: String, modelVersion: Option[Long], runtimeTypeId: Long): Future[ModelVersion] = {
    modelRepository.get(modelName).flatMap{
      case None => throw new IllegalArgumentException(s"Can't find Model with name $modelName")
      case Some(model) =>
        buildModel(model.id, modelVersion, runtimeTypeId)
    }
  }

  override def updateModel(modelName: String, modelSource: ModelSource): Future[Option[Model]] = {
    if (modelSource.isExist(modelName)) {
      // model is updated
      val modelMetadata = ModelFetcher.getModel(modelSource, modelName)
      modelRepository.get(modelMetadata.modelName).flatMap {
        case Some(oldModel) =>
          val newModel = Model(
            id = oldModel.id,
            name = modelMetadata.modelName,
            source = s"${modelSource.sourceDef.prefix}:${modelMetadata.modelName}",
            modelType = modelMetadata.modelType,
            description = None,
            modelContract = modelMetadata.contract,
            created = oldModel.created,
            updated = LocalDateTime.now()
          )
          modelRepository.update(newModel).map(_ => Some(newModel))
        case None =>
          val newModel = Model(
            id = -1,
            name = modelMetadata.modelName,
            source = s"${modelSource.sourceDef.prefix}:${modelMetadata.modelName}",
            modelType = modelMetadata.modelType,
            description = None,
            modelContract = modelMetadata.contract,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
          )
          modelRepository.create(newModel).map(x => Some(x))
      }
    } else {
      // model is deleted
      modelRepository.get(modelName).map { opt =>
        opt.map { model =>
          modelRepository.delete(model.id)
          model
        }
      }
    }
  }

  override def runtimeTypesByTag(tags: Seq[String]): Future[Seq[Runtime]] =
    runtimeTypeRepository.fetchByTags(tags)

  override def modelRuntimeByTag(tags: Seq[String]): Future[Seq[ModelVersion]] =
    modelRuntimeRepository.fetchByTags(tags)

  override def generateModelPayload(modelName: String, signature: String): Future[Seq[JsObject]] = {
    modelRepository.get(modelName).map {
      case None => throw new IllegalArgumentException(s"Can't find model modelName=$modelName")
      case Some(model) =>
        val res = DataGenerator.forContract(model.modelContract, signature).get.generateInputs
        Seq(ContractOps.TensorProtoOps.jsonify(res))
    }
  }

  override def generateInputsForRuntime(runtimeId: Long, signature: String): Future[Option[Seq[JsObject]]] = {
    modelRuntimeRepository.get(runtimeId).map(_.map{ runtime =>
      val res = DataGenerator.forContract(runtime.modelContract, signature).get.generateInputs
      Seq(ContractOps.TensorProtoOps.jsonify(res))
    })
  }

  override def submitContract(modelId: Long, prototext: String): Future[Option[Model]] = {
    ModelContract.validateAscii(prototext) match {
      case Left(a) => Future.failed(new IllegalArgumentException(a.msg))
      case Right(b) => updateModelContract(modelId, b)
    }
  }

  override def submitFlatContract(
    modelId: Long,
    contractDescription: ContractOps.ContractDescription
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

  private def updateModelContract(modelId: Long, modelContract: ModelContract): Future[Option[Model]] = {
    modelRepository.get(modelId).flatMap {
      case Some(model) =>
        val newModel = model.copy(modelContract = modelContract) // TODO contract validation (?)
        modelRepository.update(newModel).map { _ => Some(newModel) }
      case None => Future.successful(None)
    }
  }
}

package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service.modelbuild.{ModelBuildService, ModelPushService, ProgressHandler, ProgressMessage}
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.model_api.{ApiGenerator, DataFrame, ModelApi}
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.model._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class CreateRuntimeTypeRequest(
  name: String,
  version: String,
  tags: Option[List[String]],
  configParams: Option[Map[String, String]]
) {
  def toRuntimeType: RuntimeType = {
    RuntimeType(
      id = 0,
      name = this.name,
      version = this.version,
      tags = this.tags.getOrElse(List()),
      configParams = this.configParams.getOrElse(Map())
    )
  }
}

case class CreateOrUpdateModelRequest(
  id: Option[Long],
  name: String,
  source: String,
  runtimeTypeId: Option[Long],
  description: Option[String],
  outputFields: Option[ModelApi],
  inputFields: Option[ModelApi]
) {
  def toModel(runtimeType: Option[RuntimeType]): Model = {
    Model(
      id = 0,
      name = this.name,
      source = this.source,
      runtimeType = runtimeType,
      description = this.description,
      outputFields = this.outputFields.getOrElse(DataFrame(List.empty)),
      inputFields = this.inputFields.getOrElse(DataFrame(List.empty)),
      created = LocalDateTime.now(),
      updated = LocalDateTime.now()
    )
  }

  def toModel(model: Model, runtimeType: Option[RuntimeType]): Model = {
    model.copy(
      name = this.name,
      source = this.source,
      runtimeType = runtimeType,
      description = this.description,
      outputFields = this.outputFields.getOrElse(DataFrame(List.empty)),
      inputFields = this.inputFields.getOrElse(DataFrame(List.empty))
    )
  }
}

case class CreateModelRuntime(
  imageName: String,
  imageTag: String,
  imageMD5Tag: String,
  modelName: String,
  modelVersion: String,
  source: Option[String],
  runtimeTypeId: Option[Long],
  outputFields: Option[ModelApi],
  inputFields: Option[ModelApi],
  modelId: Option[Long],
  tags: Option[List[String]],
  configParams: Option[Map[String, String]]
) {
  def toModelRuntime(runtimeType: Option[RuntimeType]): ModelRuntime = {
    ModelRuntime(
      id = 0,
      imageName = this.imageName,
      imageTag = this.imageTag,
      imageMD5Tag = this.imageMD5Tag,
      modelName = this.modelName,
      modelVersion = this.modelVersion,
      source = this.source,
      runtimeType = runtimeType,
      outputFields = this.outputFields.getOrElse(DataFrame(List.empty)),
      inputFields = this.inputFields.getOrElse(DataFrame(List.empty)),
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

  def createRuntimeType(entity: CreateRuntimeTypeRequest): Future[RuntimeType]

  def buildModel(modelId: Long, modelVersion: Option[String]): Future[ModelRuntime]

  def buildModel(modelName: String, modelVersion: Option[String]): Future[ModelRuntime]

  def allRuntimeTypes(): Future[Seq[RuntimeType]]

  def runtimeTypesByTag(tags: Seq[String]): Future[Seq[RuntimeType]]

  def allModels(): Future[Seq[Model]]

  def updateModel(entity: CreateOrUpdateModelRequest): Future[Model]

  def updateModel(modelName: String, modelSource: ModelSource): Future[Option[Model]]

  def createModel(entity: CreateOrUpdateModelRequest): Future[Model]

  def deleteModel(modelName: String): Future[Model]

  def updatedInModelSource(entity: Model): Future[Unit]

  def addModelRuntime(entity: CreateModelRuntime): Future[ModelRuntime]

  def allModelRuntime(): Future[Seq[ModelRuntime]]

  def modelRuntimeByTag(tags: Seq[String]): Future[Seq[ModelRuntime]]

  def lastModelRuntimeByModel(id: Long, maximum: Int): Future[Seq[ModelRuntime]]

  def allModelBuilds(): Future[Seq[ModelBuild]]

  def modelBuildsByModel(id: Long): Future[Seq[ModelBuild]]

  def lastModelBuildsByModel(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def createFileForModel(source: ModelSource, path: String, hash: String, createdAt: LocalDateTime, updatedAt: LocalDateTime): Future[Int]

  def updateOrCreateModelFile(source: ModelSource, modelName: String, hash: String, createdAt: LocalDateTime, updatedAt: LocalDateTime): Future[Int]

  def deleteModelFile(fileName: String): Future[Int]

  def generateModelPayload(modelName: String): Future[Seq[Any]]

  def generateInputsForRuntime(runtimeId: Long): Future[Option[Seq[Any]]]
}

object ModelManagementService {
  def nextVersion(lastRuntime: Option[ModelRuntime]): String = lastRuntime match {
    case None => "0.0.1"
    case Some(runtime) =>
      val splitted = runtime.modelVersion.split('.')
      splitted.lastOption match {
        case None => runtime.modelVersion + ".1"
        case Some(v) =>
          Try.apply(v.toInt) match {
            case Failure(_) => runtime.modelVersion + ".1"
            case Success(intVersion) => splitted.dropRight(1).mkString(".") + "." + (intVersion + 1)
          }
      }
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

  override def createRuntimeType(entity: CreateRuntimeTypeRequest): Future[RuntimeType] =
    runtimeTypeRepository.create(entity.toRuntimeType)

  override def allRuntimeTypes(): Future[Seq[RuntimeType]] = runtimeTypeRepository.all()

  override def allModels(): Future[Seq[Model]] = modelRepository.all()

  override def createModel(entity: CreateOrUpdateModelRequest): Future[Model] =
    fetchRuntimeType(entity.runtimeTypeId).flatMap(runtimeType => {
      modelRepository.create(entity.toModel(runtimeType))
    })

  override def updateModel(entity: CreateOrUpdateModelRequest): Future[Model] =
    fetchRuntimeType(entity.runtimeTypeId).flatMap(runtimeType => {
      modelRepository.get(entity.id.getOrElse(throw new IllegalArgumentException("Id required for this action")))
        .flatMap({
          case None => throw new IllegalArgumentException(s"Can't find Model with id ${entity.id.get}")
          case model => modelRepository.update(entity.toModel(model.get, runtimeType))
            //TODO use returning
            .flatMap(_ => modelRepository.get(model.get.id).map(s => s.get))
        })
    })

  override def addModelRuntime(entity: CreateModelRuntime): Future[ModelRuntime] =
    fetchRuntimeType(entity.runtimeTypeId).flatMap(runtimeType => {
      modelRuntimeRepository.create(entity.toModelRuntime(runtimeType))
    })

  override def allModelRuntime(): Future[Seq[ModelRuntime]] = modelRuntimeRepository.all()

  override def lastModelRuntimeByModel(id: Long, maximum: Int): Future[Seq[ModelRuntime]] =
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

  private def buildNewModelVersion(model: Model, modelVersion: Option[String]): Future[ModelRuntime] = {
    fetchLastModelVersion(model.id, modelVersion).flatMap({ version =>
      fetchScriptForRuntime(model.runtimeType).flatMap({ script =>
        modelBuildRepository.create(ModelBuild(
          id = 0,
          model = model,
          modelVersion = version,
          started = LocalDateTime.now(),
          finished = None,
          status = ModelBuildStatus.STARTED,
          statusText = None,
          logsUrl = None,
          modelRuntime = None
        )).flatMap({ modelBuid =>
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
        })
      })
    })
  }

  override def buildModel(modelId: Long, modelVersion: Option[String]): Future[ModelRuntime] =
    modelRepository.get(modelId)
      .flatMap({
        case None => throw new IllegalArgumentException(s"Can't find Model with id $modelId")
        case Some(model) =>
          buildNewModelVersion(model, modelVersion)
      })

  private def fetchScriptForRuntime(runtimeType: Option[RuntimeType]): Future[String] = {
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

  private def buildModelRuntime(modelBuild: ModelBuild, script: String): Future[ModelRuntime] = {
    val handler = new ProgressHandler {
      override def handle(progressMessage: ProgressMessage): Unit =
        logger.info(progressMessage)
    }

    val imageName = modelPushService.getImageName(modelBuild)
    Future(modelBuildService.build(modelBuild, imageName, script, handler)).flatMap({
      md5 =>
        modelRuntimeRepository.create(ModelRuntime(
          id = 0,
          imageName = imageName,
          imageTag = modelBuild.modelVersion,
          imageMD5Tag = md5,
          modelName = modelBuild.model.name,
          modelVersion = modelBuild.modelVersion,
          source = Some(modelBuild.model.source),
          runtimeType = modelBuild.model.runtimeType,
          outputFields = modelBuild.model.outputFields,
          inputFields = modelBuild.model.inputFields,
          created = LocalDateTime.now,
          modelId = Some(modelBuild.model.id),
          tags = modelBuild.model.runtimeType.map(r => r.tags).getOrElse(List()),
          configParams = modelBuild.model.runtimeType.map(r => r.configParams).getOrElse(Map())
        )).flatMap(modelRuntime => {
          Future(modelPushService.push(modelRuntime, handler)).map(l => modelRuntime)
        })
    })
  }

  private def fetchLastModelVersion(modelId: Long, modelVersion: Option[String]): Future[String] = {
    modelVersion match {
      case Some(x) => modelRuntimeRepository.modelRuntimeByModelAndVersion(modelId, x).map {
        case None => x
        case _ => throw new IllegalArgumentException("You already have such version")
      }
      case _ => modelRuntimeRepository.lastModelRuntimeByModel(modelId, 1)
        .map(se => ModelManagementService.nextVersion(se.headOption))
    }
  }

  private def fetchRuntimeType(id: Option[Long]): Future[Option[RuntimeType]] = {
    if (id.isEmpty) {
      Future.successful(None)
    } else {
      runtimeTypeRepository.get(id.get).map({
        case None => throw new IllegalArgumentException(s"Can't find RuntimeType with id ${id.get}")
        case r => r
      })
    }
  }

  private def addModel(model: Model): Future[Model] = {
    model.runtimeType match {
      case Some(sc: SchematicRuntimeType) =>
        runtimeTypeRepository.fetchByNameAndVersion(sc.name, sc.version)
          .flatMap(runtimeType =>
            modelRepository.create(model.copy(runtimeType = runtimeType))
          )
      case _ =>
        modelRepository.create(model)
    }
  }

  def deleteModel(modelName: String): Future[Model] = {
    modelRepository.get(modelName).flatMap {
      case Some(model) =>
        modelFilesRepository.deleteModelFiles(model.id)
        modelRepository.delete(model.id)
        Future(model)
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

  override def buildModel(modelName: String, modelVersion: Option[String]): Future[ModelRuntime] =
    modelRepository.get(modelName).flatMap({
      case None => throw new IllegalArgumentException(s"Can't find Model with name $modelName")
      case Some(model) =>
        buildNewModelVersion(model, modelVersion)
    })

  override def updateModel(modelName: String, modelSource: ModelSource): Future[Option[Model]] = {
    if (modelSource.isExist(modelName)) {
      // model is updated
      val modelMetadata = ModelFetcher.getModel(modelSource, modelName)
      val fRuntime = modelMetadata.runtimeType match {
        case Some(rt) =>
          runtimeTypeRepository.fetchByNameAndVersion(rt.name, rt.version)
        case None => Future(None)
      }
      fRuntime.flatMap { rt =>
        modelRepository.get(modelMetadata.name).flatMap {
          case Some(oldModel) =>
            val newModel = Model(
              oldModel.id,
              modelMetadata.name,
              s"${modelSource.getSourcePrefix}:${modelMetadata.name}",
              rt,
              None,
              modelMetadata.outputFields,
              modelMetadata.inputFields,
              oldModel.created,
              LocalDateTime.now()
            )
            modelRepository.update(newModel).map(_ => Some(newModel))
          case None =>
            val newModel = Model(
              -1,
              modelMetadata.name,
              s"${modelSource.getSourcePrefix}:${modelMetadata.name}",
              rt,
              None,
              modelMetadata.outputFields,
              modelMetadata.inputFields,
              LocalDateTime.now(),
              LocalDateTime.now()
            )
            modelRepository.create(newModel).map(x => Some(x))
        }
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

  override def runtimeTypesByTag(tags: Seq[String]): Future[Seq[RuntimeType]] =
    runtimeTypeRepository.fetchByTags(tags)

  override def modelRuntimeByTag(tags: Seq[String]): Future[Seq[ModelRuntime]] =
    modelRuntimeRepository.fetchByTags(tags)

  override def generateModelPayload(modelName: String): Future[Seq[Any]] = {
    modelRepository.get(modelName).map {
      case None => throw new IllegalArgumentException(s"Can't find model modelName=$modelName")
      case Some(model) => List(new ApiGenerator(model.inputFields).generate)
    }
  }

  override def generateInputsForRuntime(runtimeId: Long): Future[Option[Seq[Any]]] = {
    modelRuntimeRepository.get(runtimeId).map(_.map{ runtime =>
      List(new ApiGenerator(runtime.inputFields).generate)
    })
  }
}

package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service.modelbuild.{ModelBuildService, ProgressHandler, ProgressMessage}
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


case class ModelInfo(
  model: Model,
  lastModelBuild: Option[ModelBuild],
  lastModelRuntime: Option[ModelRuntime]
)

case class CreateRuntimeTypeRequest(
  name: String,
  version: String,
  tags: Option[List[String]]
) {
  def toRuntimeType: RuntimeType = {
    RuntimeType(
      id = 0,
      name = this.name,
      version = this.version,
      tags = this.tags.getOrElse(List())
    )
  }
}

case class CreateOrUpdateModelRequest(
  id: Option[Long],
  name: String,
  source: String,
  runtimeTypeId: Option[Long],
  description: Option[String],
  outputFields: Option[List[String]],
  inputFields: Option[List[String]]
) {
  def toModel(runtimeType: Option[RuntimeType]): Model = {
    Model(
      id = 0,
      name = this.name,
      source = this.source,
      runtimeType = runtimeType,
      description = this.description,
      outputFields = this.outputFields.getOrElse(List()),
      inputFields = this.inputFields.getOrElse(List()),
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
      outputFields = this.outputFields.getOrElse(List()),
      inputFields = this.inputFields.getOrElse(List())
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
  outputFields: Option[List[String]],
  inputFields: Option[List[String]],
  modelId: Option[Long]
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
      outputFields = this.outputFields.getOrElse(List()),
      inputFields = this.inputFields.getOrElse(List()),
      created = LocalDateTime.now(),
      modelId = this.modelId
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

  def allRuntimeTypes(): Future[Seq[RuntimeType]]

  def allModels(): Future[Seq[Model]]

  def allModelsWithLastStatus(): Future[Seq[ModelInfo]]

  def updateModel(entity: CreateOrUpdateModelRequest): Future[Model]

  def createModel(entity: CreateOrUpdateModelRequest): Future[Model]

  def deleteModel(modelName: String): Future[Model]

  def updatedInModelSource(entity: Model): Future[Unit]

  def addModelRuntime(entity: CreateModelRuntime): Future[ModelRuntime]

  def allModelRuntime(): Future[Seq[ModelRuntime]]

  def lastModelRuntimeByModel(id: Long, maximum: Int): Future[Seq[ModelRuntime]]

  def allModelBuilds(): Future[Seq[ModelBuild]]

  def modelBuildsByModel(id: Long): Future[Seq[ModelBuild]]

  def lastModelBuildsByModel(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def createFileForModel(source: ModelSource, path: String, hash: String, createdAt: LocalDateTime, updatedAt: LocalDateTime): Future[ModelFile]

  def updateOrCreateModelFile(source: ModelSource, modelName: String, hash: String, createdAt: LocalDateTime, updatedAt: LocalDateTime): Future[ModelFile]

  def deleteModelFile(fileName: String): Future[Int]
}


class ModelManagementServiceImpl(
  runtimeTypeRepository: RuntimeTypeRepository,
  modelRepository: ModelRepository,
  modelFilesRepository: ModelFilesRepository,
  modelRuntimeRepository: ModelRuntimeRepository,
  modelBuildRepository: ModelBuildRepository,
  runtimeTypeBuildScriptRepository: RuntimeTypeBuildScriptRepository,
  modelBuildService: ModelBuildService
)(implicit val ex: ExecutionContext) extends ModelManagementService with Logging {

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

  override def buildModel(modelId: Long, modelVersion: Option[String]): Future[ModelRuntime] =
    modelRepository.get(modelId)
      .flatMap({
        case None => throw new IllegalArgumentException(s"Can't find Model with id $modelId")
        case Some(model) =>
          fetchLastModelVersion(modelId, modelVersion).flatMap({ version =>
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

    Future(modelBuildService.build(modelBuild, script, handler)).flatMap({
      md5 =>
        modelRuntimeRepository.create(ModelRuntime(
          id = 0,
          imageName = modelBuild.model.name,
          imageTag = modelBuild.modelVersion,
          imageMD5Tag = md5,
          modelName = modelBuild.model.name,
          modelVersion = modelBuild.modelVersion,
          source = Some(modelBuild.model.source),
          runtimeType = modelBuild.model.runtimeType,
          outputFields = modelBuild.model.outputFields,
          inputFields = modelBuild.model.inputFields,
          created = LocalDateTime.now,
          modelId = Some(modelBuild.model.id)
        ))
    })
  }

  private def fetchLastModelVersion(modelId: Long, modelVersion: Option[String]): Future[String] = {
    modelVersion match {
      case Some(x) => Future.successful(x)
      case _ => modelRuntimeRepository.lastModelRuntimeByModel(modelId, 1)
        .map(se => se.headOption match {
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
        })
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
        Future.failed(new NoSuchElementException)
    }
  }

  def createFileForModel(source: ModelSource, fileName: String, hash: String, createdAt: LocalDateTime, updatedAt: LocalDateTime): Future[ModelFile] = {
    val modelName = fileName.split("/").head
    modelRepository.get(modelName).flatMap {
      case Some(model) =>
        modelFilesRepository.create(
          ModelFile(-1, fileName, model, hash, createdAt, updatedAt)
        )
      case None =>
        val model = ModelFetcher.getModel(source, modelName)
        addModel(model)
          .flatMap { model =>
            modelFilesRepository.create(
              ModelFile(-1, fileName, model, hash, createdAt, updatedAt)
            )
          }
    }
  }

  def updateOrCreateModelFile(source: ModelSource, fileName: String, hash: String, createdAt: LocalDateTime, updatedAt: LocalDateTime): Future[ModelFile] = {
    modelFilesRepository.get(fileName).flatMap {
      case Some(modelFile) =>
        modelFilesRepository.update(
          ModelFile(
            modelFile.id,
            modelFile.path,
            modelFile.model,
            modelFile.hashSum,
            modelFile.createdAt,
            modelFile.updatedAt
          )
        )
      case None =>
        createFileForModel(source, fileName, hash, updatedAt, updatedAt)
    }
  }

  def deleteModelFile(fileName: String): Future[Int] = {
    modelFilesRepository.get(fileName).flatMap {
      case Some(modelFile) =>
        modelFilesRepository.delete(modelFile.id)
      case None =>
        logger.error(s"No such file: $fileName")
        Future.failed(new NoSuchElementException())
    }
  }

  //TODO refactor - create specific service for UI (with repository)
  override def allModelsWithLastStatus(): Future[Seq[ModelInfo]] =
    modelRepository.all().flatMap(models => {
      val ids = models.map(m => m.id)
      modelRuntimeRepository.lastModelRuntimeForModels(models.map(m => m.id)).flatMap(runtimes => {
        modelBuildRepository.lastForModels(ids).flatMap(builds => {
          Future(models.map(model => {
            ModelInfo(
              model = model,
              lastModelRuntime = runtimes.find(r => r.modelId.get == model.id),
              lastModelBuild = builds.find(b => b.model.id == model.id)
            )
          }))
        })
      })
    })
}

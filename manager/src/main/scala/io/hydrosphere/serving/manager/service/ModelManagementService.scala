package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.repository.{ModelBuildRepository, ModelRepository, ModelRuntimeRepository, RuntimeTypeRepository}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


case class CreateRuntimeTypeRequest(
  name: String,
  version: String
) {
  def toRuntimeType: RuntimeType = {
    RuntimeType(
      id = 0,
      name = this.name,
      version = this.version
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

trait ModelManagementService {

  def createRuntimeType(entity: CreateRuntimeTypeRequest): Future[RuntimeType]

  def buildModel(modelId: Long, modelVersion: Option[String]): Future[Model]

  def allRuntimeTypes(): Future[Seq[RuntimeType]]

  def allModels(): Future[Seq[Model]]

  def updateModel(entity: CreateOrUpdateModelRequest): Future[Model]

  def createModel(entity: CreateOrUpdateModelRequest): Future[Model]

  def updatedInModelSource(entity: Model): Future[Unit]

  def addModelRuntime(entity: CreateModelRuntime): Future[ModelRuntime]

  def allModelRuntime(): Future[Seq[ModelRuntime]]

  def lastModelRuntimeByModel(id: Long, maximum: Int): Future[Seq[ModelRuntime]]

  def allModelBuilds(): Future[Seq[ModelBuild]]

  def modelBuildsByModel(id: Long): Future[Seq[ModelBuild]]

  def lastModelBuildsByModel(id: Long, maximum: Int): Future[Seq[ModelBuild]]
}

class ModelManagementServiceImpl(
  runtimeTypeRepository: RuntimeTypeRepository,
  modelRepository: ModelRepository,
  modelRuntimeRepository: ModelRuntimeRepository,
  modelBuildRepository: ModelBuildRepository
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


  override def buildModel(modelId: Long, modelVersion: Option[String]): Future[Model] = ???

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

}

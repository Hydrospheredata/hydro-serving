package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.repository.{ModelBuildRepository, ModelRepository, ModelRuntimeRepository, RuntimeTypeRepository}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  *
  */

trait ModelManagementService {
  def buildModel(modelId: Long, modelVersion: Option[String]): Future[Model]

  def createRuntimeType(entity: RuntimeType): Future[RuntimeType]

  def allRuntimeTypes(): Future[Seq[RuntimeType]]

  def allModels(): Future[Seq[Model]]

  def updateModel(entity: Model): Future[Model]

  def createModel(entity: Model): Future[Model]

  def updatedInModelSource(entity: Model): Future[Unit]

  def addModelRuntime(entity: ModelRuntime): Future[ModelRuntime]

  def allModelRuntime(): Future[Seq[ModelRuntime]]

  def lastModelRuntimeByModel(id:Long, maximum:Int): Future[Seq[ModelRuntime]]

  def allModelBuilds():Future[Seq[ModelBuild]]

  def modelBuildsByModel(id:Long):Future[Seq[ModelBuild]]

  def lastModelBuildsByModel(id:Long, maximum:Int):Future[Seq[ModelBuild]]
}

class ModelManagementServiceImpl(
  runtimeTypeRepository: RuntimeTypeRepository,
  modelRepository: ModelRepository,
  modelRuntimeRepository: ModelRuntimeRepository,
  modelBuildRepository: ModelBuildRepository
)(implicit val ex: ExecutionContext) extends ModelManagementService with Logging {
  override def createRuntimeType(entity: RuntimeType): Future[RuntimeType] = runtimeTypeRepository.create(entity)

  override def allRuntimeTypes(): Future[Seq[RuntimeType]] = runtimeTypeRepository.all()

  override def allModels(): Future[Seq[Model]] = modelRepository.all()

  override def createModel(entity: Model): Future[Model] = modelRepository.create(entity)

  override def updateModel(entity: Model): Future[Model] = ???

  override def buildModel(modelId: Long, modelVersion: Option[String]): Future[Model] = ???

  override def addModelRuntime(entity: ModelRuntime): Future[ModelRuntime] = modelRuntimeRepository.create(entity)

  override def allModelRuntime(): Future[Seq[ModelRuntime]] = modelRuntimeRepository.all()

  override def lastModelRuntimeByModel(id: Long, maximum: Int): Future[Seq[ModelRuntime]] =
    modelRuntimeRepository.lastModelRuntimeByModel(id: Long, maximum: Int)

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

  override def lastModelBuildsByModel(id: Long, maximum:Int): Future[Seq[ModelBuild]] =
    modelBuildRepository.lastByModelId(id, maximum)
}

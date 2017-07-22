package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.{Model, RuntimeType, SchematicRuntimeType}
import io.hydrosphere.serving.manager.repository.{ModelRepository, RuntimeTypeRepository}
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
}

class ModelManagementServiceImpl(
  runtimeTypeRepository: RuntimeTypeRepository,
  modelRepository: ModelRepository
)(implicit val ex: ExecutionContext) extends ModelManagementService with Logging {
  override def createRuntimeType(entity: RuntimeType): Future[RuntimeType] = runtimeTypeRepository.create(entity)

  override def allRuntimeTypes(): Future[Seq[RuntimeType]] = runtimeTypeRepository.all()

  override def allModels(): Future[Seq[Model]] = modelRepository.all()

  override def createModel(entity: Model): Future[Model] = modelRepository.create(entity)

  override def updateModel(entity: Model): Future[Model] = ???

  override def buildModel(modelId: Long, modelVersion: Option[String]): Future[Model] = ???

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
}

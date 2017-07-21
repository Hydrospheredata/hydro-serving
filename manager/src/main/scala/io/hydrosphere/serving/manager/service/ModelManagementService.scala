package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.{Model, RuntimeType}
import io.hydrosphere.serving.manager.repository.{ModelRepository, RuntimeTypeRepository}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

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

  override def updatedInModelSource(entity: Model): Future[Unit] = {
    logger.debug(s"updatedInModelSource - $entity")
    modelRepository.fetchBySource(entity.source)
      .map {
        case Nil => modelRepository.create(entity).map(_ => Unit)
        case _ => modelRepository.updateLastUpdatedTime(entity.source, LocalDateTime.now())
      }

  }
}

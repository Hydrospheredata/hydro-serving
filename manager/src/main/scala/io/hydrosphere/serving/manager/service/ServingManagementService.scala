package io.hydrosphere.serving.manager.service


import io.hydrosphere.serving.manager.repository.{EndpointRepository, PipelineRepository}
import io.hydrosphere.serving.model.{Endpoint, Pipeline}

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
trait ServingManagementService {
  def deleteEndpoint(endpointId: Long): Future[Unit]

  def addEndpoint(r: Endpoint): Future[Endpoint]

  def allEndpoints(): Future[Seq[Endpoint]]

  def deletePipeline(pipelineId: Long): Future[Unit]

  def addPipeline(pipeline: Pipeline): Future[Pipeline]

  def allPipelines(): Future[Seq[Pipeline]]

}

class ServingManagementServiceImpl(
  endpointRepository: EndpointRepository,
  pipelineRepository: PipelineRepository
)(implicit val ex: ExecutionContext) extends ServingManagementService {

  override def deletePipeline(pipelineId: Long): Future[Unit] =
    pipelineRepository.delete(pipelineId).map(p => Unit)

  override def addPipeline(pipeline: Pipeline): Future[Pipeline] =
    pipelineRepository.create(pipeline)

  override def allPipelines(): Future[Seq[Pipeline]] =
    pipelineRepository.all()

  override def allEndpoints(): Future[Seq[Endpoint]] =
    endpointRepository.all()

  override def addEndpoint(r: Endpoint): Future[Endpoint] =
    endpointRepository.create(r)

  override def deleteEndpoint(endpointId: Long): Future[Unit] =
    endpointRepository.delete(endpointId).map(p => Unit)
}

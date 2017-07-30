package io.hydrosphere.serving.manager.service


import io.hydrosphere.serving.manager.repository.{EndpointRepository, PipelineRepository}
import io.hydrosphere.serving.model.{Endpoint, Pipeline}

import scala.concurrent.{ExecutionContext, Future}

case class CreateEndpointRequest(
  name: String,
  currentPipelineId: Option[Long]
) {

  def toEndpoint(pipeline: Option[Pipeline]): Endpoint = {
    Endpoint(
      endpointId = 0,
      name = this.name,
      currentPipeline = pipeline
    )
  }
}

trait ServingManagementService {
  def deleteEndpoint(endpointId: Long): Future[Unit]

  def addEndpoint(r: CreateEndpointRequest): Future[Endpoint]

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

  override def addEndpoint(r: CreateEndpointRequest): Future[Endpoint] =
    fetchPipeline(r.currentPipelineId).flatMap(pipe => {
      endpointRepository.create(r.toEndpoint(pipe))
    })

  override def deleteEndpoint(endpointId: Long): Future[Unit] =
    endpointRepository.delete(endpointId).map(p => Unit)

  private def fetchPipeline(id: Option[Long]): Future[Option[Pipeline]] = {
    if (id.isEmpty) {
      Future.successful(None)
    } else {
      pipelineRepository.get(id.get).map({
        case None => throw new IllegalArgumentException(s"Can't find Pipeline with id ${id.get}")
        case r => r
      })
    }
  }
}

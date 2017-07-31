package io.hydrosphere.serving.manager.service


import akka.http.scaladsl.model.HttpHeader
import io.hydrosphere.serving.connector.{ExecutionCommand, ExecutionUnit, RuntimeMeshConnector}
import io.hydrosphere.serving.manager.model.ModelService
import io.hydrosphere.serving.manager.repository.{EndpointRepository, ModelServiceRepository, PipelineRepository}
import io.hydrosphere.serving.model.{Endpoint, Pipeline, PipelineStage}

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

case class CreatePipelineStageRequest(
  serviceId: Long,
  servePath: Option[String]
)

case class CreatePipelineRequest(
  name: String,
  stages: Seq[CreatePipelineStageRequest]
) {
  def toPipeline(services: Seq[ModelService]): Pipeline = {
    val mappedStages = this.stages.map(st => {
      services.find(ser => ser.serviceId == st.serviceId) match {
        case None => throw new IllegalArgumentException(s"Wrong service Id=${st.serviceId}")
        case Some(service) =>
          PipelineStage(
            serviceId = st.serviceId,
            serviceName = service.serviceName,
            servePath = st.servePath.getOrElse("/serve")
          )
      }
    })
    Pipeline(
      pipelineId = 0,
      name = this.name,
      stages = mappedStages
    )
  }
}

trait ServingManagementService {
  def deleteEndpoint(endpointId: Long): Future[Unit]

  def addEndpoint(r: CreateEndpointRequest): Future[Endpoint]

  def allEndpoints(): Future[Seq[Endpoint]]

  def deletePipeline(pipelineId: Long): Future[Unit]

  def addPipeline(pipeline: CreatePipelineRequest): Future[Pipeline]

  def allPipelines(): Future[Seq[Pipeline]]

  def serveModelService(serviceId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def servePipeline(pipelineId: Long, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

}

class ServingManagementServiceImpl(
  endpointRepository: EndpointRepository,
  pipelineRepository: PipelineRepository,
  modelServiceRepository: ModelServiceRepository,
  runtimeMeshConnector: RuntimeMeshConnector
)(implicit val ex: ExecutionContext) extends ServingManagementService {

  override def deletePipeline(pipelineId: Long): Future[Unit] =
    pipelineRepository.delete(pipelineId).map(p => Unit)

  override def addPipeline(createPipelineRequest: CreatePipelineRequest): Future[Pipeline] =
    modelServiceRepository.fetchByIds(createPipelineRequest.stages.map(p => p.serviceId))
      .flatMap(services => {
        pipelineRepository.create(createPipelineRequest.toPipeline(services))
      })

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

  override def serveModelService(serviceId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]] =
    modelServiceRepository.get(serviceId).flatMap({
      case None => throw new IllegalArgumentException(s"Wrong service Id=$serviceId")
      case Some(service) => runtimeMeshConnector.execute(ExecutionCommand(
        headers = headers,
        json = request,
        pipe = Seq(ExecutionUnit(
          serviceName = service.serviceName,
          servicePath = servePath
        ))
      )).map(r => r.json)
    })

  override def servePipeline(pipelineId: Long, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]] =
    pipelineRepository.get(pipelineId).flatMap({
      case None => throw new IllegalArgumentException(s"Can't find Pipeline with id $pipelineId")
      case Some(r) =>
        runtimeMeshConnector.execute(ExecutionCommand(
          headers = headers,
          json = request,
          pipe = r.stages
            .map(s => ExecutionUnit(
              serviceName = s.serviceName,
              servicePath = s.servePath
            ))
        )).map(r => r.json)
    })

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

package io.hydrosphere.serving.manager.service


import java.util.UUID

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import io.hydrosphere.serving.connector.{ExecutionCommand, ExecutionResult, ExecutionUnit, RuntimeMeshConnector}
import io.hydrosphere.serving.model.ModelService
import io.hydrosphere.serving.model.{ServiceWeight, WeightedService}
import io.hydrosphere.serving.manager.repository.{EndpointRepository, ModelServiceRepository, PipelineRepository, WeightedServiceRepository}
import io.hydrosphere.serving.model.{Endpoint, Pipeline, PipelineStage}
import io.hydrosphere.serving.model_api.ApiGenerator

import scala.concurrent.{ExecutionContext, Future}

case class WeightedServiceCreateOrUpdateRequest(
  id: Option[Long],
  serviceName: String,
  weights: List[ServiceWeight],
  sourcesList: Option[List[Long]]
) {

  def toWeightedService: WeightedService = {
    WeightedService(
      id = this.id.getOrElse(0),
      serviceName = this.serviceName,
      weights = this.weights,
      sourcesList = this.sourcesList.getOrElse(List())
    )
  }
}

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

  def allWeightedServices(): Future[Seq[WeightedService]]

  def weightedServicesByModelServiceIds(servicesIds:Seq[Long]): Future[Seq[WeightedService]]

  def createWeightedServices(req: WeightedServiceCreateOrUpdateRequest): Future[WeightedService]

  def updateWeightedServices(req: WeightedServiceCreateOrUpdateRequest): Future[WeightedService]

  def deleteWeightedService(id: Long): Future[Unit]

  def getWeightedService(id: Long): Future[Option[WeightedService]]

  def serveWeightedService(serviceId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def deleteEndpoint(endpointId: Long): Future[Unit]

  def addEndpoint(r: CreateEndpointRequest): Future[Endpoint]

  def allEndpoints(): Future[Seq[Endpoint]]

  def deletePipeline(pipelineId: Long): Future[Unit]

  def addPipeline(pipeline: CreatePipelineRequest): Future[Pipeline]

  def allPipelines(): Future[Seq[Pipeline]]

  def serveModelService(serviceId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def serveModelServiceByModelName(modelName: String, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def serveModelServiceByModelNameAndVersion(modelName: String, modelVersion: String, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def servePipeline(pipelineId: Long, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def generateModelPayload(modelName: String, modelVersion: String): Future[Seq[Any]]

  def generateModelPayload(modelName: String): Future[Seq[Any]]
}

class ServingManagementServiceImpl(
  endpointRepository: EndpointRepository,
  pipelineRepository: PipelineRepository,
  modelServiceRepository: ModelServiceRepository,
  runtimeMeshConnector: RuntimeMeshConnector,
  weightedServiceRepository: WeightedServiceRepository,
  runtimeManagementService: RuntimeManagementService
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

  private def serveModelService(service: ModelService, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]] =
    runtimeMeshConnector.execute(ExecutionCommand(
      headers = headers,
      json = request,
      pipe = Seq(ExecutionUnit(
        serviceName = service.serviceName,
        servicePath = servePath
      ))
    )).map(mapMeshExecutionResult)

  override def serveModelService(serviceId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]] =
    modelServiceRepository.get(serviceId).flatMap({
      case None => throw new IllegalArgumentException(s"Wrong service Id=$serviceId")
      case Some(service) =>
        serveModelService(service, servePath, request, headers)
    })

  private def mapMeshExecutionResult(r: ExecutionResult): Seq[Any] = {
    r.status match {
      case StatusCodes.OK =>
        r.json
      case StatusCodes.BadRequest =>
        throw new IllegalArgumentException(r.json.toString())
      case _ =>
        throw new RuntimeException(r.json.toString())
    }
  }

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
        )).map(mapMeshExecutionResult)
    })

  private def fetchPipeline(id: Option[Long]): Future[Option[Pipeline]] = {
    if (id.isEmpty) {
      Future.successful(None)
    } else {
      pipelineRepository.get(id.get)
        .map(r => r.orElse(throw new IllegalArgumentException(s"Can't find Pipeline with id ${id.get}")))
    }
  }

  override def allWeightedServices(): Future[Seq[WeightedService]] =
    weightedServiceRepository.all()

  override def weightedServicesByModelServiceIds(servicesIds:Seq[Long]): Future[Seq[WeightedService]] =
    weightedServiceRepository.byModelServiceIds(servicesIds)

  override def createWeightedServices(req: WeightedServiceCreateOrUpdateRequest): Future[WeightedService] =
    weightedServiceRepository.create(req.toWeightedService)

  override def updateWeightedServices(req: WeightedServiceCreateOrUpdateRequest): Future[WeightedService] =
    fetchAndValidate(req.weights).flatMap(_ => {
      val service = req.toWeightedService
      weightedServiceRepository.update(service).map(_ => service)
    })


  override def deleteWeightedService(id: Long): Future[Unit] =
    weightedServiceRepository.get(id).flatMap({
      case Some(x) =>
        if (x.sourcesList.nonEmpty) {
          Future.traverse(x.sourcesList)(s => runtimeManagementService.deleteService(s))
            .flatMap(_ => weightedServiceRepository.delete(id).map(_ => Unit))
        } else {
          weightedServiceRepository.delete(id).map(_ => Unit)
        }
      case _ => Future.successful(())
    })

  private def fetchAndValidate(req: List[ServiceWeight]): Future[Unit] = {
    modelServiceRepository.fetchByIds(req.map(r => r.serviceId))
      .map(s =>
        if (s.length != req.length) {
          throw new IllegalArgumentException("Can't find all services")
        })
  }

  override def serveWeightedService(
    serviceId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]] =
    getWeightedServiceWithCheck(serviceId).flatMap(r => {
      runtimeMeshConnector.execute(ExecutionCommand(
        headers = headers,
        json = request,
        pipe = Seq(ExecutionUnit(
          serviceName = r.serviceName,
          servicePath = servePath
        ))
      )).map(mapMeshExecutionResult)
    })

  override def serveModelServiceByModelName(modelName: String, servePath: String,
    request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]] =
    modelServiceRepository.getLastModelServiceByModelName(modelName).flatMap({
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName")
      case Some(service) =>
        serveModelService(service, servePath, request, headers)
    })

  override def serveModelServiceByModelNameAndVersion(modelName: String, modelVersion: String, servePath: String,
    request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]] =
    modelServiceRepository.getLastModelServiceByModelNameAndVersion(modelName, modelVersion).flatMap({
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName and version=$modelVersion")
      case Some(service) =>
        serveModelService(service, servePath, request, headers)
    })

  override def generateModelPayload(modelName: String, modelVersion: String): Future[Seq[Any]] = {
    modelServiceRepository.getLastModelServiceByModelNameAndVersion(modelName, modelVersion).map{
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName")
      case Some(service) => List(new ApiGenerator(service.modelRuntime.inputFields).generate)
    }
  }

  override def generateModelPayload(modelName: String): Future[Seq[Any]] = {
    modelServiceRepository.getLastModelServiceByModelName(modelName).map{
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName")
      case Some(service) => List(new ApiGenerator(service.modelRuntime.inputFields).generate)
    }
  }
  private def getWeightedServiceWithCheck(serviceId: Long): Future[WeightedService] = {
    weightedServiceRepository.get(serviceId).map({
      case None => throw new IllegalArgumentException(s"Can't find WeightedService with id $serviceId")
      case Some(r) => r
    })
  }

  override def getWeightedService(id: Long): Future[Option[WeightedService]] =
    weightedServiceRepository.get(id)
}

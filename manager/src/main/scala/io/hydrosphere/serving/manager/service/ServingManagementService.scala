package io.hydrosphere.serving.manager.service

import java.util.UUID

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import io.hydrosphere.serving.connector.{ExecutionCommand, ExecutionResult, ExecutionUnit, RuntimeMeshConnector}
import io.hydrosphere.serving.model.{Application, ApplicationExecutionGraph, ModelService}
import io.hydrosphere.serving.manager.repository.{ApplicationRepository, ModelServiceRepository}
import io.hydrosphere.serving.model_api.ApiGenerator
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

case class ApplicationCreateOrUpdateRequest(
  id: Option[Long],
  serviceName: String,
  executionGraph: ApplicationExecutionGraph,
  sourcesList: Option[List[Long]]
) {

  def toApplication: Application = {
    Application(
      id = this.id.getOrElse(0),
      name = this.serviceName,
      executionGraph = this.executionGraph,
      sourcesList = this.sourcesList.getOrElse(List())
    )
  }
}

trait ServingManagementService {

  def allApplications(): Future[Seq[Application]]

  def applicationsByModelServiceIds(servicesIds: Seq[Long]): Future[Seq[Application]]

  def createApplications(req: ApplicationCreateOrUpdateRequest): Future[Application]

  def updateApplications(req: ApplicationCreateOrUpdateRequest): Future[Application]

  def deleteApplication(id: Long): Future[Unit]

  def getApplication(id: Long): Future[Option[Application]]

  def serve(req: ServeRequest): Future[ExecutionResult]

  def generateModelPayload(modelName: String, modelVersion: String): Future[Seq[Any]]

  def generateModelPayload(modelName: String): Future[Seq[Any]]
}

class ServingManagementServiceImpl(
  modelServiceRepository: ModelServiceRepository,
  runtimeMeshConnector: RuntimeMeshConnector,
  applicationRepository: ApplicationRepository,
  runtimeManagementService: RuntimeManagementService
)(implicit val ex: ExecutionContext) extends ServingManagementService with Logging {

  private def serveModelService(service: ModelService, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]] =
    runtimeMeshConnector.execute(ExecutionCommand(
      headers = headers,
      json = request,
      pipe = Seq(ExecutionUnit(
        serviceName = service.serviceName,
        servicePath = servePath
      ))
    )).map(mapMeshExecutionResult)

  override def serve(req: ServeRequest): Future[ExecutionResult] = {
    import ToPipelineStages._

    def buildStages[A](target: Option[A], error: => String)
      (implicit conv: ToPipelineStages[A]): Future[Seq[ExecutionUnit]] = {

      target match  {
        case None => Future.failed(new IllegalArgumentException(error))
        case Some(a) => Future.successful(conv.toStages(a, req.servePath))
      }
    }

    val stagesFuture = req.serviceKey match {
      case PipelineKey(id) =>
        val f = pipelineRepository.get(id)
        f.flatMap(p => buildStages(p, s"Can't find Pipeline with id: $id"))
      case WeightedKey(id) =>
        val f = getWeightedService(id)
        f.flatMap(w => buildStages(w, s"Can't find WeightedService with id: $id"))
      case WeightedName(name) =>
        val f = weightedServiceRepository.getByName(name)
        f.flatMap(w => buildStages(w, s"Can't find WeightedService with name: $name"))
      case ModelById(id) =>
        val f = modelServiceRepository.get(id)
        f.flatMap(m => buildStages(m, s"Can't find model with id: $id"))
      case ModelByName(name, None) =>
        val f = modelServiceRepository.getLastModelServiceByModelName(name)
        f.flatMap(m => buildStages(m, s"Can find model with name: $name"))
      case ModelByName(name, Some(version)) =>
        val f = modelServiceRepository.getLastModelServiceByModelNameAndVersion(name, version)
        f.flatMap(m => buildStages(m, s"Can find model with name: $name, version: $version"))
    }

    stagesFuture.flatMap(stages => {
      val cmd = ExecutionCommand(
        headers = req.headers,
        json = req.inputData,
        pipe = stages
      )
      logger.info(s"TRY INVOKE $cmd")
      runtimeMeshConnector.execute(cmd)
    })
  }

  override def allApplications(): Future[Seq[Application]] =
    applicationRepository.all()

  override def applicationsByModelServiceIds(servicesIds: Seq[Long]): Future[Seq[Application]] =
    applicationRepository.byModelServiceIds(servicesIds)

  override def createApplications(req: ApplicationCreateOrUpdateRequest): Future[Application] =
    applicationRepository.create(req.toApplication)

  override def updateApplications(req: ApplicationCreateOrUpdateRequest): Future[Application] =
    fetchAndValidate(req.executionGraph).flatMap(_ => {
      val service = req.toApplication
      applicationRepository.update(service).map(_ => service)
    })


  override def deleteApplication(id: Long): Future[Unit] =
    applicationRepository.get(id).flatMap({
      case Some(x) =>
        if (x.sourcesList.nonEmpty) {
          Future.traverse(x.sourcesList)(s => runtimeManagementService.deleteService(s))
            .flatMap(_ => applicationRepository.delete(id).map(_ => Unit))
        } else {
          applicationRepository.delete(id).map(_ => Unit)
        }
      case _ => Future.successful(())
    })

  private def fetchAndValidate(req: ApplicationExecutionGraph): Future[Unit] = {
    val servicesIds=req.stages.flatMap(s => s.services.map(c => c.serviceId))
    modelServiceRepository.fetchByIds(servicesIds)
      .map(s =>
        if (s.length != servicesIds.length) {
          throw new IllegalArgumentException("Can't find all services")
        })
  }

  override def serveApplication(
    applicationId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]] =
    getApplicationWithCheck(applicationId).flatMap(r => {
      runtimeMeshConnector.execute(ExecutionCommand(
        headers = headers,
        json = request,
        pipe = r.executionGraph.stages.indices.map(stage => ExecutionUnit(
          serviceName = s"app${r.id}stage$stage",
          servicePath = "/serve"
        ))
      )).map(mapMeshExecutionResult)
    })

  override def generateModelPayload(modelName: String, modelVersion: String): Future[Seq[Any]] = {
    modelServiceRepository.getLastModelServiceByModelNameAndVersion(modelName, modelVersion).map{
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName")
      case Some(service) => List(new ApiGenerator(service.modelRuntime.inputFields).generate)
    }
  }

  override def generateModelPayload(modelName: String): Future[Seq[Any]] = {
    modelServiceRepository.getLastModelServiceByModelName(modelName).map {
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName")
      case Some(service) => List(new ApiGenerator(service.modelRuntime.inputFields).generate)
    }
  }

  private def getApplicationWithCheck(serviceId: Long): Future[Application] = {
    applicationRepository.get(serviceId).map({
      case None => throw new IllegalArgumentException(s"Can't find Application with id $serviceId")
      case Some(r) => r
    })
  }

  override def getApplication(id: Long): Future[Option[Application]] =
    applicationRepository.get(id)
}

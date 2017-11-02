package io.hydrosphere.serving.manager.service

import java.util.UUID

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import io.hydrosphere.serving.connector.{ExecutionCommand, ExecutionResult, ExecutionUnit, RuntimeMeshConnector}
import io.hydrosphere.serving.model.{Application, ApplicationExecutionGraph, ModelService}
import io.hydrosphere.serving.manager.repository.{ApplicationRepository, ModelServiceRepository}
import io.hydrosphere.serving.model_api.{ApiCompatibilityChecker, DataGenerator, ModelApi, UntypedAPI}

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

  def serveApplication(serviceId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def serveModelService(serviceId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def serveModelServiceByModelName(modelName: String, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def serveModelServiceByModelNameAndVersion(modelName: String, modelVersion: String, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def generateModelPayload(modelName: String, modelVersion: String): Future[Seq[Any]]

  def generateModelPayload(modelName: String): Future[Seq[Any]]

  def checkApplicationSchema(req: ApplicationCreateOrUpdateRequest): Future[Boolean]

  def generateInputsForApplication(appId: Long): Future[Seq[Any]]
}

class ServingManagementServiceImpl(
  modelServiceRepository: ModelServiceRepository,
  runtimeMeshConnector: RuntimeMeshConnector,
  applicationRepository: ApplicationRepository,
  runtimeManagementService: RuntimeManagementService
)(implicit val ex: ExecutionContext) extends ServingManagementService {

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
      case Some(service) => List(DataGenerator(service.modelRuntime.inputFields).generate)
    }
  }

  override def generateModelPayload(modelName: String): Future[Seq[Any]] = {
    modelServiceRepository.getLastModelServiceByModelName(modelName).map{
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName")
      case Some(service) => List(DataGenerator(service.modelRuntime.inputFields).generate)
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

  override def checkApplicationSchema(req: ApplicationCreateOrUpdateRequest): Future[Boolean] = {
    val services = req.executionGraph.stages.map(_.services.map(_.serviceId))
    val serviceStatuses = services.zip(services.tail).map{
      case (a, b) =>
        val compMat = checkStagesSchemas(a, b)
        val stagesStatus = compMat.map(_.forall(sub => sub.forall(_ == true)))
        stagesStatus
    }
    Future.sequence(serviceStatuses).map(_.forall(_ == true))
  }

  private def checkStagesSchemas(stageA: List[Long], stageB: List[Long]): Future[List[List[Boolean]]] = {
    val servicesA = stageA.map(modelServiceRepository.get)
    val servicesB = stageB.map(modelServiceRepository.get)

    val results = servicesA.map{ fServiceA =>
      servicesB.map{ fServiceB =>
        fServiceA.zip(fServiceB).map{
          case (Some(serviceA), Some(serviceB)) =>
            checkServicesSchemas(serviceA, serviceB)
        }
      }
    }
    val r = results.map(x => Future.sequence(x))
    Future.sequence(r)
  }

  private def checkServicesSchemas(modelServiceA: ModelService, modelServiceB: ModelService): Boolean = {
    ApiCompatibilityChecker.check(modelServiceA.modelRuntime.outputFields -> modelServiceB.modelRuntime.inputFields)
  }

  override def generateInputsForApplication(appId: Long): Future[Option[Seq[Any]]] = {
    applicationRepository.get(appId).map(_.map{ app =>
      val schema = inferAppInputSchema(app)
      Seq(DataGenerator(schema).generate)
    })
  }

  private def inferAppInputSchema(application: Application): ModelApi = {
    val stages = application.executionGraph.stages.map(_.services.map(_.serviceId))
    val headServices = Future.sequence(stages.head.map(modelServiceRepository.get))
    val apis = headServices.map{services =>
      services.map{
        case Some(service) => service.modelRuntime.inputFields
        case None => throw new IllegalArgumentException(s"Can't find service in ${application.id} in $services")
      }
    }
    Future.sequence(apis)
  }
}

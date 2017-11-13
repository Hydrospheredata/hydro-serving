package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.connector._
import io.hydrosphere.serving.model.{Application, ApplicationExecutionGraph}
import io.hydrosphere.serving.manager.repository.{ApplicationRepository, ModelServiceRepository}
import io.hydrosphere.serving.model_api.{ApiCompatibilityChecker, DataGenerator, ModelApi, UntypedAPI}
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

  def checkApplicationSchema(req: ApplicationCreateOrUpdateRequest): Future[Boolean]

  def generateInputsForApplication(appId: Long): Future[Seq[Any]]
}

class ServingManagementServiceImpl(
  modelServiceRepository: ModelServiceRepository,
  runtimeMeshConnector: RuntimeMeshConnector,
  applicationRepository: ApplicationRepository,
  runtimeManagementService: RuntimeManagementService
)(implicit val ex: ExecutionContext) extends ServingManagementService with Logging {

  override def serve(req: ServeRequest): Future[ExecutionResult] = {
    import ToPipelineStages._

    def buildStages[A](target: Option[A], error: => String)
      (implicit conv: ToPipelineStages[A]): Future[Seq[ExecutionUnit]] = {

      target match {
        case None => Future.failed(new IllegalArgumentException(error))
        case Some(a) => Future.successful(conv.toStages(a, req.servePath))
      }
    }

    val stagesFuture = req.serviceKey match {
      case ApplicationKey(id) =>
        val f = getApplication(id)
        f.flatMap(w => buildStages(w, s"Can't find Application with id: $id"))
      case ApplicationName(name) =>
        val f = applicationRepository.getByName(name)
        f.flatMap(w => buildStages(w, s"Can't find Application with name: $name"))
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
    val servicesIds = req.stages.flatMap(s => s.services.map(c => c.serviceId))
    modelServiceRepository.fetchByIds(servicesIds)
      .map(s =>
        if (s.length != servicesIds.length) {
          throw new IllegalArgumentException("Can't find all services")
        })
  }

  override def generateModelPayload(modelName: String, modelVersion: String): Future[Seq[Any]] = {
    modelServiceRepository.getLastModelServiceByModelNameAndVersion(modelName, modelVersion).map {
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName")
      case Some(service) => List(DataGenerator(service.modelRuntime.inputFields).generate)
    }
  }

  override def generateModelPayload(modelName: String): Future[Seq[Any]] = {
    modelServiceRepository.getLastModelServiceByModelName(modelName).map {
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName")
      case Some(service) => List(DataGenerator(service.modelRuntime.inputFields).generate)
    }
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

package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.service.clouddriver._
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}

case class CreateServiceRequest(
  serviceName: String,
  runtimeId: Long,
  configParams: Option[Map[String, String]],
  environmentId: Option[Long],
  modelVersionId: Option[Long]
) {
  def toService(runtime: Runtime, model: Option[ModelVersion], environment: Option[Environment]): Service =
    Service(
      id = 0,
      serviceName = this.serviceName,
      cloudDriverId = None,
      runtime = runtime,
      model = model,
      statusText = "New",
      environment = environment,
      configParams = runtime.configParams ++ this.configParams.getOrElse(Map())
    )
}

case class CreateEnvironmentRequest(
  name: String,
  placeholders: Seq[Any]
) {
  def toEnvironment: Environment = {
    Environment(
      name = this.name,
      placeholders = this.placeholders,
      id = 0
    )
  }
}

case class JsonServeRequest(
  targetId: Long,
  signatureName: String,
  inputs: JsObject
)

trait ServiceManagementService {

  def deleteService(serviceId: Long): Future[Unit]

  def addService(r: CreateServiceRequest): Future[Service]

  def allServices(): Future[Seq[Service]]

  def servicesByIds(ids: Seq[Long]): Future[Seq[Service]]

  def getServicesByModel(modelId: Long): Future[Seq[Service]]

  def getServicesByRuntimes(runtimeId: Set[Long]): Future[Seq[Service]]

  def getService(serviceId: Long): Future[Option[Service]]

  def serviceByFullName(fullName: String): Future[Option[Service]]

  def allEnvironments(): Future[Seq[Environment]]

  def createEnvironment(r: CreateEnvironmentRequest): Future[Environment]

  def deleteEnvironment(environmentId: Long): Future[Unit]

  def fetchServicesUnsync(services: Set[ServiceKeyDescription]): Future[Seq[Service]]
}

//TODO ADD cache
class ServiceManagementServiceImpl(
  cloudDriverService: CloudDriverService,
  serviceRepository: ServiceRepository,
  runtimeRepository: RuntimeRepository,
  modelVersionRepository: ModelVersionRepository,
  environmentRepository: EnvironmentRepository,
  internalManagerEventsPublisher: InternalManagerEventsPublisher
)(implicit val ex: ExecutionContext) extends ServiceManagementService with Logging {

  private def mapInternalService(cloudService: CloudService): Service = {
    Service(
      id = cloudService.id,
      serviceName = cloudService.serviceName,
      cloudDriverId = Some(cloudService.cloudDriverId),
      runtime = Runtime(
        id = cloudService.runtimeInfo.runtimeId,
        name = cloudService.runtimeInfo.runtimeName,
        version = cloudService.runtimeInfo.runtimeVersion,
        suitableModelType = List(ModelType.Unknown("unknown")),
        tags = List(),
        configParams = Map()
      ),
      environment = cloudService.environmentName.map(n => Environment(
        id = -1L,
        name = n,
        placeholders = Seq()
      )),
      model = cloudService.modelInfo.map(m => ModelVersion(
        id = m.modelId,
        imageName = m.imageName,
        imageTag = m.imageTag,
        imageSHA256 = s"${m.imageName}:${m.imageTag}",
        created = LocalDateTime.now(),
        modelName = m.modelName,
        modelVersion = m.modelVersion,
        modelType = m.modelType,
        source = None,
        model = None,
        modelContract = ModelContract.defaultInstance
      )),
      statusText = cloudService.statusText,
      configParams = Map()
    )
  }

  private def syncServices(services: Seq[Service]): Future[Seq[Service]] =
    cloudDriverService.services(services.map(s => s.id).toSet).map(cloudServices => {
      val map = cloudServices.map(p => p.id -> p).toMap
      services.map(s => {
        map.get(s.id) match {
          case Some(cs) =>
            s.copy(statusText = cs.statusText)
          case _ =>
            s.copy(statusText = "Unknown")
        }
      })
    })


  override def allServices(): Future[Seq[Service]] =
    cloudDriverService.serviceList().flatMap(cloudServices => {
      serviceRepository.all().flatMap(services => {
        val map = services.map(p => p.id -> p).toMap
        Future.successful(cloudServices.map(s => {
          map.get(s.id) match {
            case Some(cs) =>
              cs.copy(statusText = s.statusText)
            case _ =>
              mapInternalService(s)
          }
        }))
      })
    })

  private def fetchModel(modelId: Option[Long]): Future[Option[ModelVersion]] = {
    logger.debug(modelId)
    modelId match {
      case Some(x) =>
        modelVersionRepository.get(x)
          .map(s => s.orElse(throw new IllegalArgumentException(s"Can't find ModelVersion with id=$x")))
      case None => Future.successful(None)
    }
  }

  private def fetchServingEnvironment(environmentId: Option[Long]): Future[Option[Environment]] = {
    logger.debug(environmentId)
    environmentId match {
      case Some(x) => x match {
        case AnyEnvironment.`anyEnvironmentId` =>
          Future.successful(None)
        case _ =>
          environmentRepository.get(x)
      }
      case None => Future.successful(None)
    }
  }

  def createAndDeploy(
    r: CreateServiceRequest,
    runtime: Runtime,
    modelVersion: Option[ModelVersion],
    svEnv: Option[Environment]
  ): Future[Service] = {
    val dService = r.toService(runtime, modelVersion, svEnv)
    serviceRepository.create(dService).flatMap { newService =>
      val future = cloudDriverService.deployService(newService).flatMap { cloudService =>
        serviceRepository.updateCloudDriveId(cloudService.id, Some(cloudService.cloudDriverId)).map { _ =>
          newService.copy(cloudDriverId = Some(cloudService.cloudDriverId))
        }
      }
      future.onFailure {
        case _ => serviceRepository.delete(dService.id)
      }
      future
    }
  }

  override def addService(r: CreateServiceRequest): Future[Service] = {
    logger.debug(r.toString)
    //TODO ADD validation for names manager,gateway + length + without space and special symbols
    for {
      svEnv <- fetchServingEnvironment(r.environmentId)
      modelVersion <- fetchModel(r.modelVersionId)
      maybeRuntime <- runtimeRepository.get(r.runtimeId)
      runtime = maybeRuntime.getOrElse(throw new IllegalArgumentException(s"Can't find Runtime with id=${r.runtimeId}"))
      asd <- createAndDeploy(r, runtime, modelVersion, svEnv)
    } yield {
      internalManagerEventsPublisher.serviceChanged(asd)
      asd
    }
  }

  override def getService(serviceId: Long): Future[Option[Service]] =
    cloudDriverService.services(Set(serviceId))
      .flatMap(opt => {
        val info = opt.headOption.getOrElse(throw new IllegalArgumentException(s"Can't find service with id $serviceId"))
        info.id match {
          case CloudDriverService.MANAGER_ID =>
            Future.successful(Some(mapInternalService(info)))
          case CloudDriverService.GATEWAY_HTTP_ID =>
            Future.successful(Some(mapInternalService(info)))
          case _ =>
            serviceRepository.get(serviceId).map({
              case Some(service) =>
                Some(service.copy(statusText = info.statusText))
              case _ => throw new IllegalArgumentException(s"Can't find service with id $serviceId")
            })
        }
      })

  //TODO check service in applications before delete
  override def deleteService(serviceId: Long): Future[Unit] =
    serviceRepository.get(serviceId).flatMap({
      case Some(x) =>
        cloudDriverService.removeService(serviceId)
          .flatMap(_ => serviceRepository.delete(serviceId)).map(_ =>
          internalManagerEventsPublisher.serviceRemoved(x)
        )
      case None =>
        Future.successful(Unit)
    })


  override def servicesByIds(ids: Seq[Long]): Future[Seq[Service]] =
    serviceRepository.fetchByIds(ids)
      .flatMap(syncServices)

  override def getServicesByModel(modelId: Long): Future[Seq[Service]] =
    serviceRepository.getByModelIds(Seq(modelId))
      .flatMap(syncServices)

  override def getServicesByRuntimes(runtimeIds: Set[Long]): Future[Seq[Service]] =
    serviceRepository.getByRuntimeIds(runtimeIds)
      .flatMap(syncServices)

  override def serviceByFullName(fullName: String): Future[Option[Service]] =
    CloudDriverService.specialNames.get(fullName) match {
      case Some(id) =>
        cloudDriverService.services(Set(id))
          .map(p => p.headOption.map(mapInternalService))
      case None => serviceRepository.getByServiceName(fullName)
    }

  override def allEnvironments(): Future[Seq[Environment]] =
    environmentRepository.all().flatMap(s => Future.successful(s :+ new AnyEnvironment()))

  override def createEnvironment(r: CreateEnvironmentRequest): Future[Environment] =
    environmentRepository.create(r.toEnvironment)

  override def deleteEnvironment(environmentId: Long): Future[Unit] =
    environmentRepository.delete(environmentId).map(_ => Unit)

  override def fetchServicesUnsync(services: Set[ServiceKeyDescription]): Future[Seq[Service]] =
    serviceRepository.fetchServices(services)

  /*override def serveService(jsonServeRequest: JsonServeRequest): Future[JsObject] =
    serviceRepository.get(jsonServeRequest.targetId)
      .flatMap(s => {
        val service = s.getOrElse(throw new IllegalArgumentException(s"Can't find service with id=${jsonServeRequest.targetId}"))
        val model = service.model.getOrElse(throw new IllegalArgumentException(s"Can't find ModelContract for service $service"))
        val validator = new PredictRequestContractValidator(model.modelContract)
        validator.convert(JsonPredictRequest(
          modelName = model.modelName,
          version = Some(model.modelVersion),
          signatureName = jsonServeRequest.signatureName,
          inputs = jsonServeRequest.inputs
        )).right.map{ grpcRequest=>{
          grpcClient
            .withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, service.serviceName)
            .predict(grpcRequest)
        }}

        Future.failed(new UnsupportedOperationException)
      })
*/
  /*
  JsonPredictRequest.fromServeRequest(req).map{ jsonRequest =>
      val validator = new PredictRequestContractValidator(??? /*ModelContract*/)
      validator.convert(jsonRequest).right.map{ grpcRequest =>
        // send to service grpcRequest
      }
    }
  */
}
package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.service.clouddriver._

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

trait ServiceManagementService {
  val MANAGER_ID: Long = -20
  val GATEWAY_ID: Long = -10
  val MANAGER_NAME: String = "manager"
  val GATEWAY_NAME: String = "gateway"

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

  def serveService(serviceId: Long, inputData: Array[Byte]): Future[Array[Byte]]

  def fetchServicesUnsync(services: Set[ServiceKeyDescription]): Future[Seq[Service]]
}

//TODO ADD cache
class ServiceManagementServiceImpl(
  cloudDriverService: CloudDriverService,
  serviceRepository: ServiceRepository,
  runtimeRepository: RuntimeRepository,
  modelVersionRepository: ModelVersionRepository,
  environmentRepository: EnvironmentRepository
)(implicit val ex: ExecutionContext) extends ServiceManagementService {

  private val specialNames = Map(
    MANAGER_NAME -> MANAGER_ID,
    GATEWAY_NAME -> GATEWAY_ID
  )

  private def mapInternalService(cloudService: CloudService): Service = {
    Service(
      id = cloudService.id,
      serviceName = cloudService.serviceName,
      cloudDriverId = Some(cloudService.cloudDriverId),
      runtime = Runtime(
        id = cloudService.runtimeInfo.runtimeId,
        name = cloudService.runtimeInfo.runtimeName,
        version = cloudService.runtimeInfo.runtimeVersion,
        suitableModelType = List(ModelType.Unknown()),
        tags = List(),
        configParams = cloudService.configParams
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
      configParams = cloudService.configParams
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
    serviceRepository.all().flatMap(syncServices)

  private def fetchModel(modelId: Option[Long]): Future[Option[ModelVersion]] =
    modelId match {
      case Some(x) =>
        modelVersionRepository.get(x)
          .map(s => s.orElse(throw new IllegalArgumentException(s"Can't find ModelVersion with id=$x")))
      case None => Future.successful(None)
    }

  private def fetchServingEnvironment(environmentId: Option[Long]): Future[Option[Environment]] = environmentId match {
    case Some(x) => x match {
      case AnyEnvironment.`anyEnvironmentId` =>
        Future.successful(None)
      case _ =>
        environmentRepository.get(x)
    }
    case None => Future.successful(None)
  }

  override def addService(r: CreateServiceRequest): Future[Service] = {
    //if(r.serviceName) throw new IllegalArgumentException
    //TODO ADD validation for names manager,gateway + length + without space and special symbols
    fetchServingEnvironment(r.environmentId).flatMap(svEnv => {
      fetchModel(r.modelVersionId).flatMap(modelVersion => {
        runtimeRepository.get(r.runtimeId).flatMap {
          case None => throw new IllegalArgumentException(s"Can't find ModelRuntime with id=${r.runtimeId}")
          case Some(runtime) =>
            serviceRepository.create(r.toService(runtime, modelVersion, svEnv))
              .flatMap(newService => {
                cloudDriverService.deployService(newService).flatMap(cloudService => {
                  serviceRepository.updateCloudDriveId(cloudService.id, Some(cloudService.cloudDriverId))
                    .map(_ => newService.copy(cloudDriverId = Some(cloudService.cloudDriverId)))
                })
              })
        }
      })
    })
  }

  override def getService(serviceId: Long): Future[Option[Service]] =
    cloudDriverService.services(Set(serviceId))
      .flatMap(opt => {
        val info = opt.headOption.getOrElse(throw new IllegalArgumentException(s"Can't find service with id $serviceId"))
        info.id match {
          case MANAGER_ID =>
            Future.successful(Some(mapInternalService(info)))
          case GATEWAY_ID =>
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
    cloudDriverService.removeService(serviceId)
      .flatMap(_ => serviceRepository.delete(serviceId)).map(_ => Unit)

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
    specialNames.get(fullName) match {
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

  override def serveService(serviceId: Long, inputData: Array[Byte]): Future[Array[Byte]] =
    Future.failed(new UnsupportedOperationException) //TODO

  override def fetchServicesUnsync(services: Set[ServiceKeyDescription]): Future[Seq[Service]] =
    serviceRepository.fetchServices(services)
}
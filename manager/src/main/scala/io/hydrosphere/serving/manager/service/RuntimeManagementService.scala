package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.model._
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.service.clouddriver._

import scala.concurrent.{ExecutionContext, Future}

case class CreateModelServiceRequest(
  serviceName: String,
  modelRuntimeId: Long,
  configParams: Option[Map[String, String]],
  environmentId: Option[Long]
) {
  def toModelService(runtime: ModelRuntime): ModelService = {
    ModelService(
      serviceId = 0,
      serviceName = this.serviceName,
      cloudDriverId = None,
      modelRuntime = runtime,
      status = None,
      statusText = None,
      environmentId = this.environmentId,
      configParams = runtime.configParams ++ this.configParams.getOrElse(Map())
    )
  }
}

case class CreateServingEnvironment(
  name: String,
  placeholders: Seq[Any]
) {
  def toServingEnvironment(): ServingEnvironment = {
    ServingEnvironment(
      name = this.name,
      placeholders = this.placeholders,
      id = 0
    )
  }
}

trait RuntimeManagementService {
  val MANAGER_ID: Long = -20
  val GATEWAY_ID: Long = -10
  val MANAGER_NAME: String = "manager"
  val GATEWAY_NAME: String = "gateway"

  def deleteService(serviceId: Long): Future[Unit]

  def instancesForService(serviceId: Long): Future[Seq[ModelServiceInstance]]

  def instancesForService(serviceName: String): Future[Seq[ModelServiceInstance]]

  def addService(r: CreateModelServiceRequest): Future[ModelService]

  def allServices(): Future[Seq[ModelService]]

  def servicesByIds(ids: Seq[Long]): Future[Seq[ModelService]]

  def getService(serviceId: Long): Future[Option[ModelService]]

  def getServicesByModel(modelId: Long): Future[Seq[ModelService]]

  def getServicesByRuntimes(runtimeId: Seq[Long]): Future[Seq[ModelService]]

  def allServingEnvironments(): Future[Seq[ServingEnvironment]]

  def createServingEnvironment(r: CreateServingEnvironment): Future[ServingEnvironment]

  def deleteServingEnvironment(environmentId: Long): Future[Unit]

}

//TODO ADD cache
class RuntimeManagementServiceImpl(
  runtimeDeployService: RuntimeDeployService,
  modelServiceRepository: ModelServiceRepository,
  modelRuntimeRepository: ModelRuntimeRepository,
  servingEnvironmentRepository: ServingEnvironmentRepository
)(implicit val ex: ExecutionContext) extends RuntimeManagementService {

  override def allServices(): Future[Seq[ModelService]] =
    Future(runtimeDeployService.serviceList())
      .flatMap(serviceInfoList => {
        modelServiceRepository.all().map(seqInDatabase => {
          mergeServiceInfo(serviceInfoList, seqInDatabase)
        })
      })

  private def mergeServiceInfo(serviceInfoList: Seq[ServiceInfo], seqInDatabase: Seq[ModelService]): Seq[ModelService] = {
    serviceInfoList.map(info => {
      info.id match {
        case MANAGER_ID =>
          this.mapServiceInfo(info, MANAGER_NAME, Map())
        case GATEWAY_ID =>
          this.mapServiceInfo(info, GATEWAY_NAME, Map())
        case _ =>
          seqInDatabase.find(s => s.serviceId == info.id) match {
            case Some(s) => s.copy(
              configParams = s.configParams ++ info.configParams,
              environmentId = s.environmentId
            )
            case None => this.mapServiceInfo(info, info.name, Map())
          }
      }
    })
  }

  private def mapServiceInfo(info: ServiceInfo, serviceName: String, configs: Map[String, String]): ModelService =
    ModelService(
      serviceId = info.id,
      serviceName = serviceName,
      cloudDriverId = Some(info.cloudDriveId),
      modelRuntime = new UnknownModelRuntime,
      environmentId = None,
      status = Some(info.status),
      statusText = Some(info.status),
      configParams = configs ++ info.configParams
    )

  override def instancesForService(serviceId: Long): Future[Seq[ModelServiceInstance]] =
    Future(runtimeDeployService.serviceInstances(serviceId))

  private def fetchPlaceholders(environmentId: Option[Long]): Future[Seq[Any]] = {
    environmentId match {
      case Some(x) => x match {
        case AnyServingEnvironment.anyServingEnvironmentId =>
          Future.successful(AnyServingEnvironment.emptyPlaceholder)
        case _ =>
          servingEnvironmentRepository.get(x)
            .map(s => s.getOrElse({
              throw new IllegalArgumentException(s"Can't find ServingEnvironment with id=$x")
            }).placeholders)
      }
      case None => Future.successful(Seq())
    }
  }

  override def addService(r: CreateModelServiceRequest): Future[ModelService] = {
    //if(r.serviceName) throw new IllegalArgumentException
    //TODO ADD validation for names manager,gateway + length + without space and special symbols
    fetchPlaceholders(r.environmentId).flatMap(pl => {
      modelRuntimeRepository.get(r.modelRuntimeId).flatMap({
        case None => throw new IllegalArgumentException(s"Can't find ModelRuntime with id=${r.modelRuntimeId}")
        case runtime => modelServiceRepository.create(r.toModelService(runtime.get)).flatMap(s =>
          Future(
            s.copy(cloudDriverId = Some(runtimeDeployService.deploy(s, pl)))
          ).flatMap(service =>
            modelServiceRepository.updateCloudDriveId(service.serviceId, service.cloudDriverId)
              .map(_ => service))
        )
      })
    })
  }

  override def instancesForService(serviceName: String): Future[Seq[ModelServiceInstance]] = {
    serviceName match {
      case MANAGER_NAME => Future(runtimeDeployService.serviceInstances(MANAGER_ID))
      case GATEWAY_NAME => Future(runtimeDeployService.serviceInstances(GATEWAY_ID))
      case _ => modelServiceRepository.getByServiceName(serviceName)
        .flatMap {
          case Some(service) =>
            Future(runtimeDeployService.serviceInstances(service.serviceId))
          case _ => throw new IllegalArgumentException(s"Can't find service for name $serviceName")
        }
    }
  }

  override def getService(serviceId: Long): Future[Option[ModelService]] =
    Future(runtimeDeployService.service(serviceId))
      .flatMap(opt => {
        val info = opt.getOrElse(throw new IllegalArgumentException(s"Can't find service with id $serviceId"))
        info.id match {
          case MANAGER_ID =>
            Future.successful(Some(this.mapServiceInfo(info, MANAGER_NAME, Map())))
          case GATEWAY_ID =>
            Future.successful(Some(this.mapServiceInfo(info, GATEWAY_NAME, Map())))
          case _ =>
            modelServiceRepository.get(serviceId).map({
              case Some(service) =>
                Some(service.copy(
                  status = Some(info.status),
                  statusText = Some(info.statusText))
                )
              case _ => throw new IllegalArgumentException(s"Can't find service with id $serviceId")
            })
        }
      })

  //TODO check service in weighted service
  override def deleteService(serviceId: Long): Future[Unit] =
    Future(runtimeDeployService.deleteService(serviceId))
      .flatMap(p => modelServiceRepository.delete(serviceId).map(p => Unit))

  //TODO Optimize - fetch special services instead of all
  override def servicesByIds(ids: Seq[Long]): Future[Seq[ModelService]] =
    allServices().map(s => s.filter(service => ids.contains(service.serviceId)))

  override def getServicesByModel(modelId: Long): Future[Seq[ModelService]] =
    allServices().map(s => s.filter(service => service.modelRuntime.modelId.fold(false)(l => l == modelId)))

  override def getServicesByRuntimes(runtimeIds: Seq[Long]): Future[Seq[ModelService]] =
    allServices().map(s => s.filter(service => runtimeIds.contains(service.modelRuntime.id)))

  override def allServingEnvironments(): Future[Seq[ServingEnvironment]] =
    servingEnvironmentRepository.all().flatMap(s => Future.successful(s :+ new AnyServingEnvironment()))

  override def createServingEnvironment(r: CreateServingEnvironment): Future[ServingEnvironment] =
    servingEnvironmentRepository.create(r.toServingEnvironment())

  override def deleteServingEnvironment(environmentId: Long): Future[Unit] =
    servingEnvironmentRepository.delete(environmentId).map(p => Unit)
}
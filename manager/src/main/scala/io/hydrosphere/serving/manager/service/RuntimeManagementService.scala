package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.model.{ModelServiceInstance, UnknownModelRuntime}
import io.hydrosphere.serving.model.{ModelRuntime, ModelService}
import io.hydrosphere.serving.manager.repository.{ModelRuntimeRepository, ModelServiceRepository}
import io.hydrosphere.serving.manager.service.clouddriver.{RuntimeDeployService, ServiceInfo}

import scala.concurrent.{ExecutionContext, Future}

case class CreateModelServiceRequest(
  serviceName: String,
  modelRuntimeId: Long,
  configParams: Option[Map[String, String]]
) {
  def toModelService(runtime: ModelRuntime): ModelService = {
    ModelService(
      serviceId = 0,
      serviceName = this.serviceName,
      cloudDriverId = None,
      modelRuntime = runtime,
      status = None,
      statusText = None,
      configParams = runtime.configParams ++ this.configParams.getOrElse(Map())
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

}

//TODO ADD cache
class RuntimeManagementServiceImpl(
  runtimeDeployService: RuntimeDeployService,
  modelServiceRepository: ModelServiceRepository,
  modelRuntimeRepository: ModelRuntimeRepository
)(implicit val ex: ExecutionContext) extends RuntimeManagementService {

  override def allServices(): Future[Seq[ModelService]] =
    Future.successful(runtimeDeployService.serviceList())
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
            case Some(s) => s.copy(configParams = s.configParams ++ info.configParams)
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
      status = Some(info.status),
      statusText = Some(info.status),
      configParams = configs ++ info.configParams
    )

  override def instancesForService(serviceId: Long): Future[Seq[ModelServiceInstance]] =
    Future(runtimeDeployService.serviceInstances(serviceId))


  override def addService(r: CreateModelServiceRequest): Future[ModelService] = {
    //if(r.serviceName) throw new IllegalArgumentException
    //TODO ADD validation for names manager,gateway + length + without space and special symbols
    modelRuntimeRepository.get(r.modelRuntimeId).flatMap {
      case None => throw new IllegalArgumentException(s"Can't find ModelRuntime with id=${r.modelRuntimeId}")
      case runtime => modelServiceRepository
        .create(r.toModelService(runtime.get))
        .map { s =>
          s.copy(cloudDriverId = Some(runtimeDeployService.deploy(s)))
        }
        .flatMap { service =>
          modelServiceRepository.updateCloudDriveId(service.serviceId, service.cloudDriverId)
            .map(_ => service)
        }
    }
  }

  override def instancesForService(serviceName: String): Future[Seq[ModelServiceInstance]] = {
    serviceName match {
      case MANAGER_NAME => Future(runtimeDeployService.serviceInstances(MANAGER_ID))
      case GATEWAY_NAME => Future(runtimeDeployService.serviceInstances(GATEWAY_ID))
      case _ => modelServiceRepository.getByServiceName(serviceName)
        .map {
          case Some(service) => runtimeDeployService.serviceInstances(service.serviceId)
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
                Some(service.copy(status = Some(info.status), statusText = Some(info.statusText)))
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
}
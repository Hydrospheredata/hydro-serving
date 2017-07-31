package io.hydrosphere.serving.manager.service

import akka.http.scaladsl.model.HttpHeader
import io.hydrosphere.serving.manager.model.{ModelRuntime, ModelService, ModelServiceInstance, UnknownModelRuntime}
import io.hydrosphere.serving.manager.repository.{ModelRuntimeRepository, ModelServiceRepository}
import io.hydrosphere.serving.manager.service.clouddriver.{RuntimeDeployService, ServiceInfo}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

case class CreateModelServiceRequest(
  serviceName: String,
  modelRuntimeId: Long
) {
  def toModelService(runtime: ModelRuntime): ModelService = {
    ModelService(
      serviceId = 0,
      serviceName = this.serviceName,
      cloudDriverId = None,
      modelRuntime = runtime,
      status = None,
      statusText = None
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

  def getService(serviceId: Long): Future[Option[ModelService]]

}

//TODO ADD cache
class RuntimeManagementServiceImpl(
  runtimeDeployService: RuntimeDeployService,
  modelServiceRepository: ModelServiceRepository,
  modelRuntimeRepository: ModelRuntimeRepository
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
          this.mapServiceInfo(info, MANAGER_NAME)
        case GATEWAY_ID =>
          this.mapServiceInfo(info, GATEWAY_NAME)
        case _ =>
          seqInDatabase.find(s => s.serviceId == info.id) match {
            case Some(s) => s
            case None => this.mapServiceInfo(info, info.name)
          }
      }
    })
  }

  private def mapServiceInfo(info: ServiceInfo, serviceName: String): ModelService =
    ModelService(
      serviceId = info.id,
      serviceName = serviceName,
      cloudDriverId = Some(info.cloudDriveId),
      modelRuntime = new UnknownModelRuntime,
      status = Some(info.status),
      statusText = Some(info.status)
    )

  override def instancesForService(serviceId: Long): Future[Seq[ModelServiceInstance]] =
    Future(runtimeDeployService.serviceInstances(serviceId))


  override def addService(r: CreateModelServiceRequest): Future[ModelService] = {
    //if(r.serviceName) throw new IllegalArgumentException
    //TODO ADD validation for names manager,gateway + length + without space and special symbols
    modelRuntimeRepository.get(r.modelRuntimeId).flatMap({
      case None=>throw new IllegalArgumentException(s"Can't find ModelRuntime with id=${r.modelRuntimeId}")
      case runtime=>modelServiceRepository.create(r.toModelService(runtime.get)).flatMap(s =>
        Future(
          s.copy(cloudDriverId = Some(runtimeDeployService.deploy(s)))
        ).flatMap(service =>
          modelServiceRepository.updateCloudDriveId(service.serviceId, service.cloudDriverId)
            .map(_ => service))
      )
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
            Future.successful(Some(this.mapServiceInfo(info, MANAGER_NAME)))
          case GATEWAY_ID =>
            Future.successful(Some(this.mapServiceInfo(info, GATEWAY_NAME)))
          case _ =>
            modelServiceRepository.get(serviceId).map({
              case Some(service) =>
                Some(service.copy(status = Some(info.status), statusText = Some(info.statusText)))
              case _ => throw new IllegalArgumentException(s"Can't find service with id $serviceId")
            })
        }
      })

  override def deleteService(serviceId: Long): Future[Unit] =
    Future(runtimeDeployService.deleteService(serviceId))
      .flatMap(p => modelServiceRepository.delete(serviceId).map(p => Unit))
  
}
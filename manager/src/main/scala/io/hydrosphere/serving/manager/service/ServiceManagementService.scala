package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.service.clouddriver._
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject
import Result.Implicits._
import io.hydrosphere.serving.manager.model.Result.ClientError

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
  def deleteService(serviceId: Long): HFResult[Service]

  def addService(r: CreateServiceRequest): HFResult[Service]

  def allServices(): Future[Seq[Service]]

  def servicesByIds(ids: Seq[Long]): Future[Seq[Service]]

  def getServicesByModel(modelId: Long): Future[Seq[Service]]

  def getServicesByRuntimes(runtimeId: Set[Long]): Future[Seq[Service]]

  def getService(serviceId: Long): HFResult[Service]

  def fetchServicesUnsync(services: Set[ServiceKeyDescription]): Future[Seq[Service]]
}

//TODO ADD cache
class ServiceManagementServiceImpl(
  cloudDriverService: CloudDriverService,
  serviceRepository: ServiceRepository,
  runtimeManagementService: RuntimeManagementService,
  versionManagementService: ModelVersionManagementService,
  environmentManagementService: EnvironmentManagementService,
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

  def createAndDeploy(
    r: CreateServiceRequest,
    runtime: Runtime,
    modelVersion: Option[ModelVersion],
    svEnv: Option[Environment]
  ): Future[Service] = {
    val env = svEnv.flatMap { app =>
      if (app.id == AnyEnvironment.id) {
        None
      } else {
        Some(app)
      }
    }
    val dService = r.toService(runtime, modelVersion, env)
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

  override def addService(r: CreateServiceRequest): HFResult[Service] = {
    logger.debug(r.toString)
    //TODO ADD validation for names manager,gateway + length + without space and special symbols
    val f = for {
      svEnv <- EitherT(extractEnvironment(r.environmentId))
      modelVersion <- EitherT(fetchModel(r.modelVersionId))
      runtime <- EitherT(runtimeManagementService.get(r.runtimeId))
      asd <- EitherT(createAndDeploy(r, runtime, modelVersion, svEnv).map(Result.ok))
    } yield {
      internalManagerEventsPublisher.serviceChanged(asd)
      asd
    }
    f.value
  }

  override def getService(serviceId: Long): HFResult[Service] = {
    val serviceRes = cloudDriverService
      .services(Set(serviceId))
      .map(_.headOption.toHResult(ClientError(s"Can't find cloud service with id $serviceId")))

    EitherT(serviceRes).flatMap { info =>
      val res = info.id match {
        case CloudDriverService.`MANAGER_ID` =>
          Result.okF(mapInternalService(info))
        case CloudDriverService.`GATEWAY_HTTP_ID` =>
          Result.okF(mapInternalService(info))

        case _ =>
          serviceRepository
            .get(serviceId)
            .map(s => s.map(_.copy(statusText = info.statusText)))
            .map(_.toHResult(ClientError(s"Can't find service with id $serviceId")))
      }
      EitherT(res)
    }.value
  }

  //TODO check service in applications before delete
  override def deleteService(serviceId: Long): HFResult[Service] =
    serviceRepository.get(serviceId).flatMap {
      case Some(x) =>
        cloudDriverService.removeService(serviceId).flatMap { _ =>
          serviceRepository.delete(serviceId)
        }.map { _ =>
          internalManagerEventsPublisher.serviceRemoved(x)
          Result.ok(x)
        }
      case None =>
        Result.clientErrorF("Can't find service")
    }

  override def servicesByIds(ids: Seq[Long]): Future[Seq[Service]] =
    serviceRepository.fetchByIds(ids)
      .flatMap(syncServices)

  override def getServicesByModel(modelId: Long): Future[Seq[Service]] =
    serviceRepository.getByModelIds(Seq(modelId))
      .flatMap(syncServices)

  override def getServicesByRuntimes(runtimeIds: Set[Long]): Future[Seq[Service]] =
    serviceRepository.getByRuntimeIds(runtimeIds)
      .flatMap(syncServices)

  override def fetchServicesUnsync(services: Set[ServiceKeyDescription]): Future[Seq[Service]] =
    serviceRepository.fetchServices(services)

  private def fetchModel(modelId: Option[Long]): HFResult[Option[ModelVersion]] = {
    logger.debug(modelId)
    modelId match {
      case Some(x) => versionManagementService.get(x).map(_.right.map(Some.apply))
      case None => Result.okF(None)
    }
  }

  private def extractEnvironment(envId: Option[Long]): HFResult[Option[Environment]] = {
    envId match {
      case Some(id) =>
        environmentManagementService.get(id).map { res =>
          res.right.map(Some.apply)
        }
      case None => Result.okF(None)
    }
  }
}
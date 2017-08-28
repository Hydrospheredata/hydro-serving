package io.hydrosphere.serving.manager.service

import akka.http.scaladsl.model.HttpHeader
import io.hydrosphere.serving.manager.model.{Model, ModelBuild, ModelRuntime, ModelService}
import io.hydrosphere.serving.manager.repository.{ModelBuildRepository, ModelRepository, ModelRuntimeRepository, ModelServiceRepository}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

case class ModelInfo(
  model: Model,
  lastModelBuild: Option[ModelBuild],
  lastModelRuntime: Option[ModelRuntime],
  currentServices: List[ModelService]
)

trait UIManagementService {

  def allModelsWithLastStatus(): Future[Seq[ModelInfo]]

  def modelWithLastStatus(modelId: Long): Future[Option[ModelInfo]]

  def stopAllServices(modelId: Long): Future[Unit]

  def testModel(modelId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def buildModel(modelId: Long, modelVersion: Option[String]): Future[ModelInfo]
}

class UIManagementServiceImpl(
  modelRepository: ModelRepository,
  modelRuntimeRepository: ModelRuntimeRepository,
  modelBuildRepository: ModelBuildRepository,
  modelServiceRepository: ModelServiceRepository,
  runtimeManagementService: RuntimeManagementService,
  servingManagementService: ServingManagementService,
  modelManagementService: ModelManagementService
)(implicit val ex: ExecutionContext) extends UIManagementService with Logging {

  //TODO Optimize implementation
  override def allModelsWithLastStatus(): Future[Seq[ModelInfo]] =
    modelRepository.all().flatMap(models => {
      val ids = models.map(m => m.id)
      modelRuntimeRepository.lastModelRuntimeForModels(models.map(m => m.id)).flatMap(runtimes => {
        modelServiceRepository.getByModelIds(ids).flatMap(services => {
          modelBuildRepository.lastForModels(ids).flatMap(builds => {
            Future(models.map(model => {
              ModelInfo(
                model = model,
                lastModelRuntime = runtimes.find(r => r.modelId.get == model.id),
                lastModelBuild = builds.find(b => b.model.id == model.id),
                currentServices = services.filter(s => s.modelRuntime.modelId.get == model.id).toList
              )
            }))
          })
        })
      })
    })


  override def modelWithLastStatus(modelId: Long): Future[Option[ModelInfo]] =
    modelRepository.get(modelId).flatMap({
      case Some(x) =>
        modelRuntimeRepository.lastModelRuntimeByModel(modelId, 1).flatMap(modelRuntimes => {
          modelServiceRepository.getByModelIds(Seq(modelId)).flatMap(services => {
            modelBuildRepository.lastByModelId(modelId, 1).map(builds => {
              Some(ModelInfo(
                model = x,
                lastModelRuntime = modelRuntimes.headOption,
                lastModelBuild = builds.headOption,
                currentServices = services.toList
              ))
            })
          })
        })
      case None => Future.successful(None)
    })


  override def stopAllServices(modelId: Long): Future[Unit] =
    modelServiceRepository.getByModelIds(Seq(modelId)).flatMap(services => {
      Future.traverse(services)(s =>
        runtimeManagementService.deleteService(s.serviceId)
          .flatMap(_ => waitForContainerStop(s))).map(s => Unit)
    })

  override def testModel(modelId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]] =
    modelServiceRepository.getByModelIds(Seq(modelId)).flatMap(services => {
      val serviceFuture = services.headOption match {
        case None => startAndWaitService(modelId)
        case Some(x) => Future.successful(x)
      }
      serviceFuture.flatMap(service => {
        servingManagementService.serveModelService(service.serviceId, servePath, request, headers)
      })
    })


  private def startAndWaitService(modelId: Long): Future[ModelService] =
    modelRuntimeRepository.lastModelRuntimeByModel(modelId, 1).flatMap(runtimes => {
      runtimes.headOption match {
        case None => throw new IllegalArgumentException("Can't find runtime for model")
        case Some(x) =>
          runtimeManagementService.addService(CreateModelServiceRequest(
            serviceName = x.modelName,
            modelRuntimeId = x.id
          )).flatMap(res => {
            waitForContainerStart(res).map(c => res)
          })
      }
    })

  //TODO add /health url checking
  private def waitForContainerStart(service: ModelService): Future[Unit] = {
    Future(Thread.sleep(10000L))
  }

  //TODO check instances
  private def waitForContainerStop(service: ModelService): Future[Unit] = {
    Future(Thread.sleep(5000L))
  }

  override def buildModel(modelId: Long, modelVersion: Option[String]): Future[ModelInfo] =
    stopAllServices(modelId).flatMap(_ => {
      modelManagementService.buildModel(modelId, modelVersion).flatMap(runtime => {
        runtimeManagementService.addService(CreateModelServiceRequest(
          serviceName = runtime.modelName,
          modelRuntimeId = runtime.id
        )).flatMap(_ => modelWithLastStatus(modelId).map(o => o.get))
      })
    })

}

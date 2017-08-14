package io.hydrosphere.serving.manager.service

import akka.http.scaladsl.model.HttpHeader
import io.hydrosphere.serving.manager.model.{Model, ModelBuild, ModelRuntime, ModelService}
import io.hydrosphere.serving.manager.repository.{ModelBuildRepository, ModelRepository, ModelRuntimeRepository, ModelServiceRepository}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{Await, ExecutionContext, Future}

case class ModelInfo(
  model: Model,
  lastModelBuild: Option[ModelBuild],
  lastModelRuntime: Option[ModelRuntime],
  currentServices: List[ModelService]
)

trait UIManagementService {

  def allModelsWithLastStatus(): Future[Seq[ModelInfo]]

  def stopAllServices(modelId: Long): Future[Unit]

  def testModel(modelId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]
}

class UIManagementServiceImpl(
  modelRepository: ModelRepository,
  modelRuntimeRepository: ModelRuntimeRepository,
  modelBuildRepository: ModelBuildRepository,
  modelServiceRepository: ModelServiceRepository,
  runtimeManagementService: RuntimeManagementService,
  servingManagementService: ServingManagementService
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

  override def stopAllServices(modelId: Long): Future[Unit] =
    modelServiceRepository.getByModelIds(Seq(modelId)).flatMap(services => {
      Future.traverse(services)(s =>
        runtimeManagementService.deleteService(s.serviceId)).map(s => Unit)
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
            Future(Thread.sleep(10000L)).map(c => res)
          })
      }
    })

}

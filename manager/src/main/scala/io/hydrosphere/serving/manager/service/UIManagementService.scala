package io.hydrosphere.serving.manager.service

import java.util.UUID

import akka.http.scaladsl.model.HttpHeader
import io.hydrosphere.serving.model.{ModelRuntime, ModelService, ServiceWeight, WeightedService}
import io.hydrosphere.serving.manager.model.{Model, ModelBuild}
import io.hydrosphere.serving.manager.repository.{ModelBuildRepository, ModelRepository, ModelRuntimeRepository, ModelServiceRepository}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

case class UIServiceInfo(
  service: ModelService,
  weightedServices: Seq[WeightedService]
)

case class UIRuntimeInfo(
  runtime: ModelRuntime,
  services: Seq[UIServiceInfo]
)

case class ModelInfo(
  model: Model,
  lastModelBuild: Option[ModelBuild],
  lastModelRuntime: Option[ModelRuntime],
  currentServices: List[ModelService],
  nextVersion: String,
  nextVersionAvailable: Boolean
)

case class KafkaStreamingParams(
  serviceId: Option[Long],
  sourceTopic: String,
  destinationTopic: String,
  brokerList: List[String]
)

case class UIWeightedServiceCreateOrUpdateRequest(
  id: Option[Long],
  serviceName: String,
  weights: List[ServiceWeight],
  kafkaStreamingSources: Option[List[KafkaStreamingParams]]
) {

  def toWeightedService: WeightedServiceCreateOrUpdateRequest = {
    WeightedServiceCreateOrUpdateRequest(
      id = this.id,
      serviceName = this.serviceName,
      weights = this.weights
    )
  }
}

case class ServiceWeightDetails(
  service: Option[ModelService],
  weight: Int
)

case class WeightedServiceDetails(
  id: Long,
  serviceName: String,
  weights: List[ServiceWeightDetails],
  kafkaStreamingSources: List[KafkaStreamingParams]
)

trait UIManagementService {

  def createWeightedService(req: UIWeightedServiceCreateOrUpdateRequest): Future[WeightedServiceDetails]

  def updateWeightedService(req: UIWeightedServiceCreateOrUpdateRequest): Future[WeightedServiceDetails]

  def allWeightedServicesDetails(): Future[Seq[WeightedServiceDetails]]

  def allModelsWithLastStatus(): Future[Seq[ModelInfo]]

  def modelWithLastStatus(modelId: Long): Future[Option[ModelInfo]]

  def stopAllServices(modelId: Long): Future[Unit]

  def testModel(modelId: Long, servePath: String, request: Seq[Any], headers: Seq[HttpHeader]): Future[Seq[Any]]

  def buildModel(modelId: Long, modelVersion: Option[String]): Future[ModelInfo]

  def modelRuntimes(modelId: Long): Future[Seq[UIRuntimeInfo]]
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
            Future(
              models.map(model => {
                val lastModelRuntime = runtimes.find(r => r.modelId.get == model.id)
                ModelInfo(
                  model = model,
                  lastModelRuntime = lastModelRuntime,
                  lastModelBuild = builds.find(b => b.model.id == model.id),
                  currentServices = services.filter(s => s.modelRuntime.modelId.get == model.id).toList,
                  nextVersion = ModelManagementService.nextVersion(lastModelRuntime),
                  nextVersionAvailable = model.created.isBefore(model.updated)
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
              val lastModelRuntime = modelRuntimes.headOption
              Some(ModelInfo(
                model = x,
                lastModelRuntime = lastModelRuntime,
                lastModelBuild = builds.headOption,
                currentServices = services.toList,
                nextVersion = ModelManagementService.nextVersion(lastModelRuntime),
                nextVersionAvailable = x.created.isBefore(x.updated)
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
            modelRuntimeId = x.id,
            configParams = None
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
    modelManagementService.buildModel(modelId, modelVersion).flatMap(runtime => {
      runtimeManagementService.addService(CreateModelServiceRequest(
        serviceName = runtime.modelName,
        modelRuntimeId = runtime.id,
        configParams = None
      )).flatMap(_ => modelWithLastStatus(modelId).map(o => o.get))
    })

  private def getDefaultKafkaImplementation(): Future[Option[ModelRuntime]] = {
    modelRuntimeRepository.fetchByTags(Seq("streaming", "kafka"))
      .map(s => s.headOption)
  }

  private def createKafkaForWeightedService(service: WeightedService, list: List[KafkaStreamingParams]): Future[WeightedServiceDetails] = {
    if (list.isEmpty) {
      fetchWeightedServiceById(service.id)
    } else {
      getDefaultKafkaImplementation().flatMap {
        case Some(x) =>
          Future.traverse(list)(streaming => {
            val configs = Map(
              "STREAMING_SOURCE_TOPIC" -> streaming.sourceTopic,
              "STREAMING_DESTINATION_TOPIC" -> streaming.destinationTopic,
              "STREAMING_BOOTSTRAP_SERVERS" -> streaming.brokerList.mkString(","),
              "STREAMING_PROCESSOR_ROUTE" -> s"weightedservices${service.id}",
              "STREAMING_KAFKA_GROUP_ID" -> UUID.randomUUID().toString
            )
            servingManagementService.addTrafficSourceToWeightedService(service.id, x.id, Option(configs))
          }).flatMap(_ => fetchWeightedServiceById(service.id))
        case _ =>
          //TODO move this check to createWeightedService
          logger.error("Can't find runtime for kafka streaming")
          fetchWeightedServiceById(service.id)
      }
    }
  }

  private def fetchWeightedServiceById(id: Long): Future[WeightedServiceDetails] = {
    servingManagementService.getWeightedService(id)
      .flatMap({
        case None => throw new IllegalArgumentException(s"Can't find service with id=$id")
        case Some(x) => mapWeighetdService(x)
      })
  }

  private def updateKafkaForWeightedService(service: WeightedService, list: List[KafkaStreamingParams]): Future[WeightedServiceDetails] = {
    val oldServices = list.filter(s => s.serviceId.nonEmpty).map(s => s.serviceId.get)

    val servicesToDelete = service.sourcesList.filter(s => !oldServices.contains(s))
    if (servicesToDelete.nonEmpty) {
      Future.traverse(servicesToDelete)(s =>
        servingManagementService.removeTrafficSourceFromWeightedService(service.id, s)
      ) onFailure {
        case thr => logger.error("Can't delete all sources ", thr)
      }
    }
    createKafkaForWeightedService(service, list.filter(s => s.serviceId.isEmpty))
  }

  override def createWeightedService(req: UIWeightedServiceCreateOrUpdateRequest): Future[WeightedServiceDetails] =
    servingManagementService.createWeightedServices(req.toWeightedService).flatMap(service => {
      req.kafkaStreamingSources match {
        case Some(x) => createKafkaForWeightedService(service, x)
        case _ => mapWeighetdService(service)
      }
    })


  override def updateWeightedService(req: UIWeightedServiceCreateOrUpdateRequest): Future[WeightedServiceDetails] =
    servingManagementService.updateWeightedServices(req.toWeightedService).flatMap(service => {
      req.kafkaStreamingSources match {
        case Some(x) => updateKafkaForWeightedService(service, x)
        case _ => updateKafkaForWeightedService(service, List())
      }
    })

  private def mapService(weighted: Seq[WeightedService], services: Seq[ModelService]): Future[Seq[WeightedServiceDetails]] = {
    Future({
      val mapService = services.map(v => v.serviceId -> v).toMap
      weighted.map(w => {
        WeightedServiceDetails(
          id = w.id,
          serviceName = w.serviceName,
          weights = w.weights.map(weight => ServiceWeightDetails(
            service = mapService.get(weight.serviceId),
            weight = weight.weight
          )),
          kafkaStreamingSources = w.sourcesList.map(stream => {
            val kafkaService = mapService.get(stream)
            kafkaService match {
              case Some(x) =>
                KafkaStreamingParams(
                  serviceId = Some(x.serviceId),
                  sourceTopic = x.configParams.getOrElse("STREAMING_SOURCE_TOPIC", "???"),
                  destinationTopic = x.configParams.getOrElse("STREAMING_DESTINATION_TOPIC", "???"),
                  brokerList = x.configParams.getOrElse("STREAMING_BOOTSTRAP_SERVERS", "???").split(',').toList
                )
              case None =>
                KafkaStreamingParams(
                  serviceId = Some(stream),
                  sourceTopic = "???",
                  destinationTopic = "???",
                  brokerList = List("???")
                )
            }
          })
        )
      })
    })
  }

  private def mapWeightedService(services: Seq[WeightedService]): Future[Seq[WeightedServiceDetails]] = {
    runtimeManagementService.servicesByIds(
      services.flatMap(s => s.weights.map(w => w.serviceId)) ++ services.flatMap(s => s.sourcesList)
    ).flatMap(ms => {
      mapService(services, ms)
    })
  }

  private def mapWeighetdService(service: WeightedService): Future[WeightedServiceDetails] = {
    mapWeightedService(Seq(service)).map(s => s.head)
  }

  override def allWeightedServicesDetails(): Future[Seq[WeightedServiceDetails]] =
    servingManagementService.allWeightedServices().flatMap(services => {
      mapWeightedService(services)
    })

  override def modelRuntimes(modelId: Long): Future[Seq[UIRuntimeInfo]] =
    for (
      runtimes <- modelRuntimeRepository.lastModelRuntimeByModel(modelId, Int.MaxValue);
      services <- runtimeManagementService.getServicesByModel(modelId);
      ws <- servingManagementService.weightedServicesByModelServiceIds(services.map(s => s.serviceId))
    ) yield {
      val wsIndex = ws.flatMap(s => s.sourcesList.map(i => i -> s))
        .groupBy(_._1).mapValues(_.map(_._2))
      val serviceIndex = services.map(s => s.modelRuntime.id -> s)
        .groupBy(_._1).mapValues(_.map(_._2))

      runtimes.map(r => {
        UIRuntimeInfo(
          runtime = r,
          services = serviceIndex.get(r.id) match {
            case None => Seq()
            case Some(x) => x.map(s => {
              UIServiceInfo(
                service = s,
                weightedServices = wsIndex.getOrElse(s.serviceId, Seq())
              )
            })
          }
        )
      })
    }
}

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

case class UIServiceWeight(
  runtimeId: Long,
  weight: Int
)

case class UIWeightedServiceCreateOrUpdateRequest(
  id: Option[Long],
  serviceName: String,
  weights: List[UIServiceWeight],
  kafkaStreamingSources: Option[List[KafkaStreamingParams]]
) {

  def toWeightedService(sourcesList: List[Long], weights: List[ServiceWeight]): WeightedServiceCreateOrUpdateRequest = {
    WeightedServiceCreateOrUpdateRequest(
      id = this.id,
      serviceName = this.serviceName,
      weights = weights,
      sourcesList = Option(sourcesList)
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
          runtimeManagementService.addService(createModelServiceRequest(x)).flatMap(res => {
            waitForContainerStart(res).map(c => res)
          })
      }
    })

  private def createModelServiceRequest(x:ModelRuntime):CreateModelServiceRequest={
    CreateModelServiceRequest(
      serviceName = s"${x.modelName}_${x.modelVersion}".replaceAll("\\.", "-"),
      modelRuntimeId = x.id,
      configParams = None,
      environmentId = None
    )
  }

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
      runtimeManagementService.addService(createModelServiceRequest(runtime)).flatMap(_ => modelWithLastStatus(modelId).map(o => o.get))
    })

  private def getDefaultKafkaImplementation(): Future[Option[ModelRuntime]] = {
    modelRuntimeRepository.fetchByTags(Seq("streaming", "kafka"))
      .map(s => s.headOption)
  }


  private def addKafkaStreaming(streaming: KafkaStreamingParams, service: WeightedService, runtimeId: Long): Future[WeightedService] = {
    val configs = Map(
      "STREAMING_SOURCE_TOPIC" -> streaming.sourceTopic,
      "STREAMING_DESTINATION_TOPIC" -> streaming.destinationTopic,
      "STREAMING_BOOTSTRAP_SERVERS" -> streaming.brokerList.mkString(","),
      "STREAMING_PROCESSOR_ROUTE" -> s"weightedservices${service.id}",
      "STREAMING_KAFKA_GROUP_ID" -> UUID.randomUUID().toString
    )
    runtimeManagementService.addService(CreateModelServiceRequest(
      serviceName = UUID.randomUUID().toString,
      modelRuntimeId = runtimeId,
      configParams = Option(configs),
      environmentId = None
    )).flatMap(kafkaService => {
      servingManagementService.updateWeightedServices(
        WeightedServiceCreateOrUpdateRequest(
          id = Some(service.id),
          serviceName = service.serviceName,
          weights = service.weights,
          sourcesList = Some(kafkaService.serviceId :: service.sourcesList)
        )
      ).map(_ => service)
    })
  }

  private def createKafkaForWeightedService(service: WeightedService, list: List[KafkaStreamingParams]): Future[WeightedServiceDetails] = {
    if (list.isEmpty) {
      fetchWeightedServiceById(service.id)
    } else {
      getDefaultKafkaImplementation().flatMap {
        case Some(x) =>
          //Add service one by one
          var fAccum = addKafkaStreaming(list.head, service, x.id)
          for (item <- list.drop(1)) {
            fAccum = fAccum.flatMap(res => {
              addKafkaStreaming(item, res, x.id)
            })
          }
          fAccum.flatMap(_ => fetchWeightedServiceById(service.id))
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

  private def removeKafka(service: WeightedService, kafkaServiceId: Long): Future[WeightedService] =
    runtimeManagementService.deleteService(kafkaServiceId).flatMap(_ => {
      servingManagementService.updateWeightedServices(
        WeightedServiceCreateOrUpdateRequest(
          id = Some(service.id),
          serviceName = service.serviceName,
          weights = service.weights,
          sourcesList = Some(service.sourcesList.filter(v => v != kafkaServiceId))
        )
      )
    })

  private def updateKafkaForWeightedService(service: WeightedService, list: List[KafkaStreamingParams]): Future[WeightedServiceDetails] = {
    val oldServices = list
      .filter(s => s.serviceId.nonEmpty)
      .filter(s => s.serviceId.get > 0)
      .map(s => s.serviceId.get)

    val servicesToDelete = service.sourcesList.filter(s => !oldServices.contains(s))

    val actualService = if (servicesToDelete.nonEmpty) {
      var fAccum = removeKafka(service, servicesToDelete.head)
      for (item <- servicesToDelete.drop(1)) {
        fAccum = fAccum.flatMap(res => {
          removeKafka(res, item)
        })
      }
      fAccum
    } else {
      Future.successful(service)
    }

    actualService.flatMap(actualService => {
      createKafkaForWeightedService(actualService, list.filter(s => s.serviceId.isEmpty))
    })
  }

  private def createServiceForRuntime(runtimeId: Long, runtimeToService: Map[Long, Long]): Future[Map[Long, Long]] = {
    modelRuntimeRepository.get(runtimeId).flatMap {
      case None => throw new IllegalArgumentException(s"Can't find runtime with id=$runtimeId")
      case Some(x) => runtimeManagementService.addService(createModelServiceRequest(x)).map(ser => {
        runtimeToService + (runtimeId -> ser.serviceId)
      })
    }
  }

  private def checkAndStartRuntime(weights: List[UIServiceWeight]): Future[List[ServiceWeight]] = {
    //TODO optimize, avoid cycle and contains
    val runtimesIds = weights.map(w => w.runtimeId)
    runtimeManagementService.getServicesByRuntimes(runtimesIds)
      .flatMap(services => {
        val runtimeToService = services.map(s => s.modelRuntime.id -> s.serviceId).toMap
        val toCreate = runtimesIds.filterNot(r => runtimeToService.contains(r))

        val fWithMappings = {
          if (toCreate.nonEmpty) {
            var fAccum = createServiceForRuntime(toCreate.head, runtimeToService)
            for (item <- toCreate.drop(1)) {
              fAccum = fAccum.flatMap(res => {
                createServiceForRuntime(item, res)
              })
            }
            fAccum
          } else {
            Future.successful(runtimeToService)
          }
        }

        fWithMappings.map(index => {
          weights.map(w => ServiceWeight(
            weight = w.weight,
            serviceId = index.getOrElse(w.runtimeId, throw new RuntimeException(s"Can't find service for runtimeId=$w"))
          ))
        })
      })
  }

  override def createWeightedService(req: UIWeightedServiceCreateOrUpdateRequest): Future[WeightedServiceDetails] =
    checkAndStartRuntime(req.weights).flatMap(weights => {
      servingManagementService.createWeightedServices(req.toWeightedService(List(), weights))
        .flatMap(service => {
          req.kafkaStreamingSources match {
            case Some(x) => createKafkaForWeightedService(service, x)
            case _ => mapWeighetdService(service)
          }
        })
    })


  override def updateWeightedService(req: UIWeightedServiceCreateOrUpdateRequest): Future[WeightedServiceDetails] =
    servingManagementService.getWeightedService(req.id match {
      case None => throw new IllegalArgumentException(s"ID required")
      case Some(r) => r
    }).flatMap {
      case None => throw new IllegalArgumentException(s"Can't find service with id=${req.id}")
      case Some(orgServ) => checkAndStartRuntime(req.weights).flatMap(weights => {
        servingManagementService.updateWeightedServices(req.toWeightedService(orgServ.sourcesList, weights))
          .flatMap(service => {
            req.kafkaStreamingSources match {
              case Some(x) => updateKafkaForWeightedService(service, x)
              case _ => updateKafkaForWeightedService(service, List())
            }
          })
      })


    }


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
      val wsIndex = ws.flatMap(s => s.weights.map(i => i.serviceId -> s))
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

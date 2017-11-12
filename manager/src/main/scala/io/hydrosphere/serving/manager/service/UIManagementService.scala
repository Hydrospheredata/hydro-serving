package io.hydrosphere.serving.manager.service

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader
import io.hydrosphere.serving.model._
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.connector._
import io.hydrosphere.serving.manager.repository.{ModelBuildRepository, ModelRepository, ModelRuntimeRepository, ModelServiceRepository}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

case class UIServiceInfo(
  service: ModelService,
  applications: Seq[Application]
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


case class UIApplicationCreateOrUpdateRequest(
  id: Option[Long],
  serviceName: String,
  stages: List[List[UIServiceWeight]],
  kafkaStreamingSources: Option[List[KafkaStreamingParams]]
) {

  def toApplication(sourcesList: List[Long], stages: List[List[ServiceWeight]]): ApplicationCreateOrUpdateRequest = {
    ApplicationCreateOrUpdateRequest(
      id = this.id,
      serviceName = this.serviceName,
      executionGraph = ApplicationExecutionGraph(
        stages = stages.map(l => {
          ApplicationStage(
            services = l
          )
        })
      ),
      sourcesList = Option(sourcesList)
    )
  }
}

case class ServiceWeightDetails(
  service: Option[ModelService],
  weight: Int
)

case class ApplicationDetails(
  id: Long,
  serviceName: String,
  stages: List[List[ServiceWeightDetails]],
  kafkaStreamingSources: List[KafkaStreamingParams]
)

trait UIManagementService {

  def createApplication(req: UIApplicationCreateOrUpdateRequest): Future[ApplicationDetails]

  def updateApplication(req: UIApplicationCreateOrUpdateRequest): Future[ApplicationDetails]

  def allApplicationsDetails(): Future[Seq[ApplicationDetails]]

  def allModelsWithLastStatus(): Future[Seq[ModelInfo]]

  def modelWithLastStatus(modelId: Long): Future[Option[ModelInfo]]

  def stopAllServices(modelId: Long): Future[Unit]

  def testModel(modelId: Long, servePath: String, request: Array[Byte], headers: Seq[HttpHeader]): Future[ExecutionResult]

  def buildModel(modelId: Long, modelVersion: Option[String], environmentId:Option[Long]): Future[ModelInfo]

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
)(implicit val ex: ExecutionContext, val actorSystem: ActorSystem, val timeout: Timeout) extends UIManagementService with Logging {
  private val containerWatcher = actorSystem.actorOf(ContainerWatcher.props)

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
                nextVersionAvailable = lastModelRuntime match {
                  case None => true
                  case Some(lastMR) => x.updated.isAfter(lastMR.created)
                }
              ))
            })
          })
        })
      case None => Future.successful(None)
    })


  override def stopAllServices(modelId: Long): Future[Unit] =
    modelServiceRepository
      .getByModelIds(Seq(modelId))
      .flatMap{ services =>
        Future.traverse(services) { s =>
          runtimeManagementService
            .deleteService(s.serviceId)
            .flatMap(_ => containerWatcher ? WatchForStop(s))
        }
      }.mapTo[Unit]

  override def testModel(modelId: Long, servePath: String, request: Array[Byte], headers: Seq[HttpHeader]): Future[ExecutionResult] =
    modelServiceRepository.getByModelIds(Seq(modelId)).flatMap(services => {
      val serviceFuture = services.headOption match {
        case None => startAndWaitService(modelId, None)
        case Some(x) => Future.successful(x)
      }
      serviceFuture.flatMap(service => {
        val serveRequest = ServeRequest(
          serviceKey = ModelById(service.serviceId),
          servePath = "/serve",
          headers = headers,
          inputData = request
        )
        servingManagementService.serve(serveRequest)
      })
    })


  private def startAndWaitService(modelId: Long, environmentId:Option[Long]): Future[ModelService] =
    modelRuntimeRepository
      .lastModelRuntimeByModel(modelId, 1)
      .flatMap { runtimes =>
        runtimes.headOption match {
          case None => throw new IllegalArgumentException("Can't find runtime for model")
          case Some(x) =>
            runtimeManagementService
              .addService(createModelServiceRequest(x, environmentId))
              .flatMap { res =>
                val f = containerWatcher ? WatchForStart(res)
                f.mapTo[Started].map(_.modelService)
              }
        }
      }

  private def createModelServiceRequest(x:ModelRuntime, environmentId:Option[Long]):CreateModelServiceRequest={
    CreateModelServiceRequest(
      serviceName = s"${x.modelName}_${x.modelVersion}".replaceAll("\\.", "-"),
      modelRuntimeId = x.id,
      configParams = None,
      environmentId = environmentId
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

  override def buildModel(modelId: Long, modelVersion: Option[String], environmentId:Option[Long]): Future[ModelInfo] =
    modelManagementService.buildModel(modelId, modelVersion).flatMap(runtime => {
      runtimeManagementService.addService(createModelServiceRequest(runtime, environmentId)).flatMap(_ => modelWithLastStatus(modelId).map(o => o.get))
    })

  private def getDefaultKafkaImplementation(): Future[Option[ModelRuntime]] = {
    modelRuntimeRepository.fetchByTags(Seq("streaming", "kafka"))
      .map(s => s.headOption)
  }


  private def addKafkaStreaming(streaming: KafkaStreamingParams, application: Application, runtimeId: Long): Future[Application] = {
    val configs = Map(
      "STREAMING_SOURCE_TOPIC" -> streaming.sourceTopic,
      "STREAMING_DESTINATION_TOPIC" -> streaming.destinationTopic,
      "STREAMING_BOOTSTRAP_SERVERS" -> streaming.brokerList.mkString(","),
      "STREAMING_PROCESSOR_APPLICATION" -> application.id.toString,
      "STREAMING_KAFKA_GROUP_ID" -> UUID.randomUUID().toString
    )
    runtimeManagementService.addService(CreateModelServiceRequest(
      serviceName = UUID.randomUUID().toString,
      modelRuntimeId = runtimeId,
      configParams = Option(configs),
      environmentId = None
    )).flatMap(kafkaService => {
      servingManagementService.updateApplications(
        ApplicationCreateOrUpdateRequest(
          id = Some(application.id),
          serviceName = application.name,
          executionGraph = application.executionGraph,
          sourcesList = Some(kafkaService.serviceId :: application.sourcesList)
        )
      ).map(_ => application)
    })
  }

  private def createKafkaForApplication(service: Application, list: List[KafkaStreamingParams]): Future[ApplicationDetails] = {
    if (list.isEmpty) {
      fetchApplicationById(service.id)
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
          fAccum.flatMap(_ => fetchApplicationById(service.id))
        case _ =>
          //TODO move this check to createApplication
          logger.error("Can't find runtime for kafka streaming")
          fetchApplicationById(service.id)
      }
    }
  }

  private def fetchApplicationById(id: Long): Future[ApplicationDetails] = {
    servingManagementService.getApplication(id)
      .flatMap({
        case None => throw new IllegalArgumentException(s"Can't find service with id=$id")
        case Some(x) => mapWeighetdService(x)
      })
  }

  private def removeKafka(service: Application, kafkaServiceId: Long): Future[Application] =
    runtimeManagementService.deleteService(kafkaServiceId).flatMap(_ => {
      servingManagementService.updateApplications(
        ApplicationCreateOrUpdateRequest(
          id = Some(service.id),
          serviceName = service.name,
          executionGraph = service.executionGraph,
          sourcesList = Some(service.sourcesList.filter(v => v != kafkaServiceId))
        )
      )
    })

  private def updateKafkaForApplication(service: Application, list: List[KafkaStreamingParams]): Future[ApplicationDetails] = {
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
      createKafkaForApplication(actualService, list.filter(s => s.serviceId.isEmpty))
    })
  }

  private def createServiceForRuntime(runtimeId: Long, runtimeToService: Map[Long, Long]): Future[Map[Long, Long]] = {
    modelRuntimeRepository.get(runtimeId).flatMap {
      case None => throw new IllegalArgumentException(s"Can't find runtime with id=$runtimeId")
      case Some(x) => runtimeManagementService.addService(createModelServiceRequest(x, None)).map(ser => {
        runtimeToService + (runtimeId -> ser.serviceId)
      })
    }
  }

  private def checkAndStartRuntime(stages: List[List[UIServiceWeight]]): Future[List[List[ServiceWeight]]] = {
    //TODO optimize, avoid cycle and contains

    val runtimesIds = stages.flatMap(s => s.map(c => c.runtimeId))
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
          stages.map(s => {
            s.map(w => ServiceWeight(
              weight = w.weight,
              serviceId = index.getOrElse(w.runtimeId, throw new RuntimeException(s"Can't find service for runtimeId=$w"))
            ))
          })
        })
      })
  }

  override def createApplication(req: UIApplicationCreateOrUpdateRequest): Future[ApplicationDetails] =
    checkAndStartRuntime(req.stages).flatMap(weights => {
      servingManagementService.createApplications(req.toApplication(List(), weights))
        .flatMap(service => {
          req.kafkaStreamingSources match {
            case Some(x) => createKafkaForApplication(service, x)
            case _ => mapWeighetdService(service)
          }
        })
    })


  override def updateApplication(req: UIApplicationCreateOrUpdateRequest): Future[ApplicationDetails] =
    servingManagementService.getApplication(req.id match {
      case None => throw new IllegalArgumentException(s"ID required")
      case Some(r) => r
    }).flatMap {
      case None => throw new IllegalArgumentException(s"Can't find service with id=${req.id}")
      case Some(orgServ) => checkAndStartRuntime(req.stages).flatMap(weights => {
        servingManagementService.updateApplications(req.toApplication(orgServ.sourcesList, weights))
          .flatMap(service => {
            req.kafkaStreamingSources match {
              case Some(x) => updateKafkaForApplication(service, x)
              case _ => updateKafkaForApplication(service, List())
            }
          })
      })


    }


  private def mapService(applications: Seq[Application], services: Seq[ModelService]): Future[Seq[ApplicationDetails]] = {
    Future({
      val mapService = services.map(v => v.serviceId -> v).toMap
      applications.map(w => {
        ApplicationDetails(
          id = w.id,
          serviceName = w.name,
          stages = w.executionGraph.stages.map(stage => stage.services
            .map(weight => ServiceWeightDetails(
              service = mapService.get(weight.serviceId),
              weight = weight.weight
            ))),
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

  private def mapApplication(services: Seq[Application]): Future[Seq[ApplicationDetails]] = {
    runtimeManagementService.servicesByIds(
      services.flatMap(s => s.executionGraph.stages.flatMap(s => s.services.map(c => c.serviceId))) ++ services.flatMap(s => s.sourcesList)
    ).flatMap(ms => {
      mapService(services, ms)
    })
  }

  private def mapWeighetdService(service: Application): Future[ApplicationDetails] = {
    mapApplication(Seq(service)).map(s => s.head)
  }

  override def allApplicationsDetails(): Future[Seq[ApplicationDetails]] =
    servingManagementService.allApplications().flatMap(services => {
      mapApplication(services)
    })

  override def modelRuntimes(modelId: Long): Future[Seq[UIRuntimeInfo]] =
    for (
      runtimes <- modelRuntimeRepository.lastModelRuntimeByModel(modelId, Int.MaxValue);
      services <- runtimeManagementService.getServicesByModel(modelId);
      ws <- servingManagementService.applicationsByModelServiceIds(services.map(s => s.serviceId))
    ) yield {

      val wsIndex = ws.flatMap(g => g.executionGraph.stages.flatMap(s => s.services.map(c => c.serviceId -> g)))
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
                applications = wsIndex.getOrElse(s.serviceId, Seq())
              )
            })
          }
        )
      })
    }
}

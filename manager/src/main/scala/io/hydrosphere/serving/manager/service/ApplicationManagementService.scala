package io.hydrosphere.serving.manager.service

import io.grpc.Context
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, KafkaTopicServerInterceptor}
import io.hydrosphere.serving.manager.ApplicationConfig
import io.hydrosphere.serving.manager.controller.application._
import io.hydrosphere.serving.manager.grpc.manager.AuthorityReplacerInterceptor
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.DataGenerator
import io.hydrosphere.serving.manager.model.api.ops.{ModelSignatureOps, TensorProtoOps}
import io.hydrosphere.serving.manager.model.api.validation.SignatureValidator
import io.hydrosphere.serving.manager.repository.{ApplicationRepository, ModelVersionRepository, RuntimeRepository}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import org.apache.logging.log4j.scala.Logging
import spray.json.{JsObject, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class ServiceWithSignature(s: Service, signatureName: String)

case class ExecutionStep(
  serviceName: String,
  serviceSignature: String
)

case class ExecutionUnit(
  serviceName: String,
  servicePath: String
)

trait ApplicationManagementService {
  def serveJsonApplication(jsonServeRequest: JsonServeRequest): Future[JsValue]

  def serveGrpcApplication(data: PredictRequest): Future[PredictResponse]

  def allApplications(): Future[Seq[Application]]

  def getApplication(id: Long): Future[Option[Application]]

  def generateInputsForApplication(appId: Long, signatureName: String): Future[Option[Seq[JsObject]]]

  def createApplication(req: CreateApplicationRequest): Future[Application]

  def deleteApplication(id: Long): Future[Unit]

  def updateApplication(req: UpdateApplicationRequest): Future[Application]
}

class ApplicationManagementServiceImpl(
  applicationRepository: ApplicationRepository,
  modelVersionRepository: ModelVersionRepository,
  serviceManagementService: ServiceManagementService,
  grpcClient: PredictionServiceGrpc.PredictionServiceStub,
  internalManagerEventsPublisher: InternalManagerEventsPublisher,
  applicationConfig: ApplicationConfig,
  runtimeRepository: RuntimeRepository
)(implicit val ex: ExecutionContext) extends ApplicationManagementService with Logging {

  type FutureMap[T] = Future[Map[Long, T]]

  //TODO REMOVE!
  def sendToDebug(request: PredictResponse, predictRequest: PredictRequest): Unit = {
    if(applicationConfig.shadowingOn){
      val req=PredictRequest(
        modelSpec=predictRequest.modelSpec,
        inputs=request.outputs
      )

      grpcClient
        .withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, CloudDriverService.GATEWAY_KAFKA_NAME)
        .withOption(KafkaTopicServerInterceptor.KAFKA_TOPIC_KEY, "shadow_topic") //TODO where can i get this
        .predict(req)
        .onComplete({
          case Failure(thr)=>
            logger.error("Can't send message to GATEWAY_KAFKA", thr)
          case _=>
            Unit
        })
    }
  }

  def serve(unit: ExecutionUnit, request: PredictRequest): Future[PredictResponse] = {
    grpcClient
      .withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, unit.serviceName)
      .predict(request)
  }

  def servePipeline(units: Seq[ExecutionUnit], data: PredictRequest): Future[PredictResponse] = {
    val empty = Future.successful(PredictResponse(outputs = data.inputs))
    units.foldLeft(empty) {
      case (a, b) =>
        a.flatMap { resp =>
          val request = PredictRequest(
            modelSpec = Some(
              ModelSpec(
                signatureName = b.servicePath
              )
            ),
            inputs = resp.outputs
          )
          serve(b, request)
        }
    }
  }

  def serveApplication(application: Application, request: PredictRequest): Future[PredictResponse] = {
    application.executionGraph.stages match {
      case stage :: Nil if stage.services.lengthCompare(1) == 0 => // single stage with single service
        val unit = ExecutionUnit(
          serviceName = ApplicationStage.stageId(application.id, 0),
          servicePath = request.modelSpec.getOrElse(throw new IllegalArgumentException(s"ModelSpec in request is not specified")).signatureName
        )
        serve(unit, request)
      case stages => // pipeline
        val execUnits = stages.zipWithIndex.map {
          case (stage, idx) => ExecutionUnit(
            serviceName = ApplicationStage.stageId(application.id, idx),
            servicePath = stage.signature.getOrElse(throw new IllegalArgumentException(s"$stage doesn't have a signature")).signatureName
          )
        }
        servePipeline(execUnits, request)
    }
  }

  def serveGrpcApplication(data: PredictRequest): Future[PredictResponse] = {
    data.modelSpec match {
      case Some(modelSpec) =>
        applicationRepository.getByName(modelSpec.name).flatMap {
          case Some(app) =>
            serveApplication(app, data)
          case None => Future.failed(new IllegalArgumentException(s"Application '${modelSpec.name}' is not found"))
        }
      case None => Future.failed(new IllegalArgumentException("ModelSpec is not defined"))
    }
  }

  def serveJsonApplication(jsonServeRequest: JsonServeRequest): Future[JsValue] = {
    applicationRepository.get(jsonServeRequest.targetId)
      .flatMap {
        case Some(application) =>
          val signature = application.contract.signatures
            .find(_.signatureName == jsonServeRequest.signatureName)
            .getOrElse(throw new IllegalArgumentException(s"Application ${jsonServeRequest.targetId} doesn't have a ${jsonServeRequest.signatureName} signature"))

          val ds = new SignatureValidator(signature).convert(jsonServeRequest.inputs).right.map { tensors =>
            PredictRequest(
              modelSpec = Some(
                ModelSpec(
                  name = application.name,
                  signatureName = jsonServeRequest.signatureName,
                  version = None
                )
              ),
              inputs = tensors
            )
          }.right.map { grpcRequest =>
            serveApplication(application, grpcRequest)
          }

          ds match {
            case Left(l) =>
              Future.failed(new IllegalArgumentException(s"Can't map request $l"))
            case Right(r) =>
              r.map(TensorProtoOps.jsonify)
          }
        case None =>
          Future.failed(new IllegalArgumentException(s"Can't find Application with id=${jsonServeRequest.targetId}"))
      }
  }

  def allApplications(): Future[Seq[Application]] = {
    val futureApps = applicationRepository.all()



    def extractServiceField[E](extractor: ServiceKeyDescription => E):Future[Seq[E]] = futureApps.map( apps =>
      for{
        app <- apps
        stage <- app.executionGraph.stages
        service <- stage.services
      } yield extractor(service.serviceDescription)
    )

    def groupBy[K,V](seq:Seq[V])(idExtractor:V => K):Map[K,V] = seq
      .groupBy(idExtractor(_))
      .mapValues(_.headOption)
      .filter(_._2.isDefined)
      .mapValues(_.get)

    val futureModelIds = extractServiceField{_.modelVersionId}.map(_.filter(_.isDefined).map(_.get))

    val modelsAndVersionsById:FutureMap[ModelVersion] = futureModelIds
      .flatMap(modelVersionRepository.modelVersionsByModelVersionIds)
      .map{groupBy(_){_.modelVersion}}

    val runtimesById:FutureMap[Runtime] = runtimeRepository.all().map(groupBy(_){_.id})

    enrichServiceKeyDescription(futureApps, runtimesById, modelsAndVersionsById)
  }

  def enrichServiceKeyDescription(futureApps:Future[Seq[Application]],
                                  futureRuntimes:FutureMap[Runtime],
                                  futureModels:FutureMap[ModelVersion]) = {

    def enrichApps(apps:Seq[Application])
               (enrich:ServiceKeyDescription => ServiceKeyDescription):Seq[Application] = apps.map{
      app => app.copy(
        executionGraph = app.executionGraph.copy(
          stages = app.executionGraph.stages.map{
            stage => stage.copy(
              services = stage.services.map{
                service => service.copy(
                  serviceDescription = enrich(service.serviceDescription)
                )
              }
            )
          }
        )
      )
    }

    for {
      apps <- futureApps
      modelData <- futureModels
      runtimeData <- futureRuntimes
    } yield enrichApps(apps){
      key => key.copy(
          modelName = key.modelVersionId.flatMap(modelData.get(_).map(_.modelName)),
          runtimeName = runtimeData.get(key.runtimeId).map(_.name)
        )
    }
  }


  def getApplication(id: Long): Future[Option[Application]] =
    applicationRepository.get(id)


  def generateInputsForApplication(appId: Long, signatureName: String): Future[Option[Seq[JsObject]]] = {
    applicationRepository.get(appId).map {
      case Some(app) =>
        app.contract.signatures.find(_.signatureName == signatureName).map { signature =>
          val data = DataGenerator(signature).generateInputs
          Seq(TensorProtoOps.jsonify(data))
        }
      case None =>
        None
    }
  }

  def createApplication(req: CreateApplicationRequest): Future[Application] = executeWithSync {
    val keys = for {
      stage <- req.executionGraph.stages
      service <- stage.services
    } yield {
      service.toDescription
    }
    val keySet = keys.toSet

    for {
      services <- serviceManagementService.fetchServicesUnsync(keySet)
      existedServices = services.map(_.toServiceKeyDescription)
      _ <- startServices(keySet -- existedServices)
      inferredApp <- composeApp(req)
      createdApp <- applicationRepository.create(inferredApp)
    } yield {
      internalManagerEventsPublisher.applicationChanged(createdApp)
      createdApp
    }
  }

  def deleteApplication(id: Long): Future[Unit] =
    executeWithSync {
      applicationRepository.get(id).flatMap {
        case Some(application) =>
          val keysSet = application.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet
          applicationRepository.delete(id)
            .flatMap(_ =>
              removeServiceIfNeeded(keysSet, id)
                .map(_ => internalManagerEventsPublisher.applicationRemoved(application))
            )
        case _ =>
          Future.successful(Unit)
      }
    }

  def updateApplication(req: UpdateApplicationRequest): Future[Application] =
    executeWithSync {
      applicationRepository.get(req.id)
        .flatMap {
          case Some(application) =>
            val keysSetOld = application.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet
            val keysSetNew = req.executionGraph.stages.flatMap(_.services.map(_.toDescription)).toSet

            for {
              _ <- removeServiceIfNeeded(keysSetOld -- keysSetNew, application.id)
              _ <- startServices(keysSetNew -- keysSetOld)
              app <- composeApp(req)
              _ <- applicationRepository.update(app)
            } yield {
              internalManagerEventsPublisher.applicationChanged(app)
              app
            }
          case None =>
            throw new IllegalArgumentException(s"Can't find application $req")
        }
    }

  private def executeWithSync[A](func: => Future[A]): Future[A] = {
    applicationRepository.getLockForApplications().flatMap { lockInfo =>
      func andThen {
        case Success(r) =>
          applicationRepository.returnLockForApplications(lockInfo)
            .map(_ => r)
        case Failure(f) =>
          applicationRepository.returnLockForApplications(lockInfo)
            .map(_ => throw f)
      }
    }
  }

  private def startServices(keysSet: Set[ServiceKeyDescription]): Future[Seq[Service]] = {
    logger.debug(keysSet)
    serviceManagementService.fetchServicesUnsync(keysSet).flatMap { services =>
      val toAdd = keysSet -- services.map(_.toServiceKeyDescription)

      Future.traverse(toAdd.toSeq) { key =>
        serviceManagementService.addService(CreateServiceRequest(
          serviceName = key.toServiceName(),
          runtimeId = key.runtimeId,
          configParams = None,
          environmentId = key.environmentId,
          modelVersionId = key.modelVersionId
        ))
      }
    }
  }

  private def removeServiceIfNeeded(keysSet: Set[ServiceKeyDescription], applicationId: Long): Future[Unit] =
    applicationRepository.getKeysNotInApplication(keysSet, applicationId)
      .flatMap(apps => {
        val keysSetOld = apps.flatMap(_.executionGraph.stages.flatMap(_.services.map(_.serviceDescription))).toSet
        serviceManagementService.fetchServicesUnsync(keysSet -- keysSetOld)
      })
      .flatMap(services => Future.traverse(services)(serv => {
        serviceManagementService.deleteService(serv.id)
      }).map(_ => Unit))

  private def composeApp(appReq: CreateApplicationRequest): Future[Application] = {
    for {
      graph <- inferGraph(appReq.executionGraph)
      contract <- inferAppContract(appReq.name, graph)
    } yield {
      Application(
        id = 0,
        name = appReq.name,
        contract = contract,
        executionGraph = graph,
        kafkaStreaming = appReq.kafkaStreaming
      )
    }
  }

  private def composeApp(appReq: UpdateApplicationRequest): Future[Application] = {
    for {
      graph <- inferGraph(appReq.executionGraph)
      contract <- inferAppContract(appReq.name, graph)
    } yield {
      Application(
        id = appReq.id,
        name = appReq.name,
        contract = contract,
        executionGraph = graph,
        kafkaStreaming = appReq.kafkaStream.getOrElse(Seq.empty).toList
      )
    }
  }

  private def inferGraph(executionGraphRequest: ExecutionGraphRequest): Future[ApplicationExecutionGraph] = {
    val appStages =
      executionGraphRequest.stages match {
        case singleStage :: Nil if singleStage.services.lengthCompare(1) == 0 =>
          inferSimpleApp(singleStage)
        case stages =>
          inferPipelineApp(stages)
      }
    appStages.map { sigs =>
      ApplicationExecutionGraph(sigs.toList)
    }
  }

  private def inferSimpleApp(singleStage: ExecutionStepRequest) = {
    Future.successful {
      Seq(
        ApplicationStage(
          services = singleStage.services.map(_.toWeighedService),
          signature = None
        )
      )
    }
  }

  private def inferPipelineApp(stages: Seq[ExecutionStepRequest]) = {
    Future.sequence {
      stages.zipWithIndex.map {
        case (stage, id) =>
          for {
            services <- inferServices(stage.services)
            stageSig <- inferStageSignature(services)
          } yield {
            ApplicationStage(
              services = services,
              signature = Some(stageSig.withSignatureName(id.toString))
            )
          }
      }
    }
  }

  def inferServices(services: List[SimpleServiceDescription]): Future[List[WeightedService]] = {
    Future.sequence {
      services.map { s =>
        val versionId = s.modelVersionId.getOrElse(throw new IllegalArgumentException(s"$s doesn't have a modelversion"))
        modelVersionRepository.get(versionId).map {
          case Some(version) =>
            val signature = version.modelContract.signatures
              .find(_.signatureName == s.signatureName)
              .getOrElse(throw new IllegalArgumentException(s"$s doesn't contain such signature"))
            s.toWeighedService.copy(signature = Some(signature))
          case None => throw new IllegalArgumentException(s"$s contains non-existant modelversion")
        }
      }
    }
  }

  private def inferAppContract(applicationName: String, graph: ApplicationExecutionGraph): Future[ModelContract] = {
    logger.debug(applicationName)
    graph.stages match {
      case stage :: Nil if stage.services.lengthCompare(1) == 0 => // single model version
        val serviceDesc = stage.services.head
        serviceManagementService.fetchServicesUnsync(Set(serviceDesc.serviceDescription)).map { services =>
          services.head.model match {
            case Some(model) => model.modelContract
            case None => throw new IllegalArgumentException(s"Service $serviceDesc has no related model.")
          }
        }
      case _ =>
        Future.successful {
          ModelContract(
            applicationName,
            Seq(inferPipelineSignature(applicationName, graph))
          )
        }
    }
  }

  private def inferStageSignature(serviceDescs: Seq[WeightedService]): Future[ModelSignature] = {
    Future {
      val signatures = serviceDescs.map { service =>
        service.signature.getOrElse(throw new IllegalArgumentException(s"$service doesn't have a signature"))
      }
      signatures.foldRight(ModelSignature.defaultInstance) {
        case (sig1, sig2) => ModelSignatureOps.merge(sig1, sig2)
      }
    }
  }

  private def inferPipelineSignature(name: String, graph: ApplicationExecutionGraph): ModelSignature = {
    ModelSignature(
      name,
      graph.stages.head.signature.get.inputs,
      graph.stages.last.signature.get.outputs
    )
  }
}

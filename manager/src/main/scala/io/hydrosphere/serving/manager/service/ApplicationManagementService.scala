package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.DataGenerator
import io.hydrosphere.serving.contract.utils.ops.ModelSignatureOps
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, KafkaTopicServerInterceptor}
import io.hydrosphere.serving.manager.ApplicationConfig
import io.hydrosphere.serving.manager.controller.application._
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.json.TensorJsonLens
import io.hydrosphere.serving.manager.model.api.tensor_builder.SignatureBuilder
import io.hydrosphere.serving.manager.repository.{ApplicationRepository, RuntimeRepository}
import io.hydrosphere.serving.manager.service.clouddriver.CloudDriverService
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import io.hydrosphere.serving.tensorflow.tensor.TypedTensorFactory
import org.apache.logging.log4j.scala.Logging
import spray.json.{JsObject, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import Result.Implicits._
import io.hydrosphere.serving.manager.model.Result.ClientError

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
  def serveJsonApplication(jsonServeRequest: JsonServeRequest): HFResult[JsValue]

  def serveGrpcApplication(data: PredictRequest): HFResult[PredictResponse]

  def allApplications(): Future[Seq[Application]]

  def getApplication(id: Long): HFResult[Application]

  def generateInputsForApplication(appId: Long, signatureName: String): HFResult[JsObject]

  def createApplication(req: CreateApplicationRequest): HFResult[Application]

  def deleteApplication(id: Long): HFResult[Application]

  def updateApplication(req: UpdateApplicationRequest): HFResult[Application]
}

class ApplicationManagementServiceImpl(
  applicationRepository: ApplicationRepository,
  modelVersionManagementService: ModelVersionManagementService,
  serviceManagementService: ServiceManagementService,
  grpcClient: PredictionServiceGrpc.PredictionServiceStub,
  internalManagerEventsPublisher: InternalManagerEventsPublisher,
  applicationConfig: ApplicationConfig,
  runtimeRepository: RuntimeRepository
)(implicit val ex: ExecutionContext) extends ApplicationManagementService with Logging {

  type FutureMap[T] = Future[Map[Long, T]]

  //TODO REMOVE!
  private def sendToDebug(request: PredictResponse, predictRequest: PredictRequest): Unit = {
    if (applicationConfig.shadowingOn) {
      val req = PredictRequest(
        modelSpec = predictRequest.modelSpec,
        inputs = request.outputs
      )

      grpcClient
        .withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, CloudDriverService.GATEWAY_KAFKA_NAME)
        .withOption(KafkaTopicServerInterceptor.KAFKA_TOPIC_KEY, "shadow_topic") //TODO where can i get this
        .predict(req)
        .onComplete {
          case Failure(thr) =>
            logger.error("Can't send message to GATEWAY_KAFKA", thr)
          case _ =>
            Unit
        }
    }
  }

  def serve(unit: ExecutionUnit, request: PredictRequest): Future[PredictResponse] = {
    grpcClient
      .withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, unit.serviceName)
      .predict(request)
      .map { response =>
        sendToDebug(response, request)
        response
      }
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

  def serveApplication(application: Application, request: PredictRequest): HFResult[PredictResponse] = {
    application.executionGraph.stages match {
      case stage :: Nil if stage.services.lengthCompare(1) == 0 => // single stage with single service
        request.modelSpec match {
          case Some(servicePath) =>
            val unit = ExecutionUnit(
              serviceName = ApplicationStage.stageId(application.id, 0),
              servicePath = servicePath.signatureName
            )
            serve(unit, request).map(Result.ok)
          case None => Result.clientErrorF("ModelSpec in request is not specified")
        }
      case stages => // pipeline
        val execUnits = stages.zipWithIndex.map {
          case (stage, idx) =>
            stage.signature match {
              case Some(signature) =>
                Result.ok(
                  ExecutionUnit(
                    serviceName = ApplicationStage.stageId(application.id, idx),
                    servicePath = signature.signatureName
                  )
                )
              case None => Result.clientError(s"$stage doesn't have a signature")
            }
        }

        val errors = execUnits.filter(_.isLeft)
        if (errors.nonEmpty) {
          Result.clientErrorF(s"Encountered errors: $errors") // TODO multiple errors?
        } else {
          val units = execUnits.map(_.right.get)
          servePipeline(units, request).map(Result.ok)
        }
    }
  }

  def serveGrpcApplication(data: PredictRequest): HFResult[PredictResponse] = {
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

  def serveJsonApplication(jsonServeRequest: JsonServeRequest): HFResult[JsObject] = {
    applicationRepository.get(jsonServeRequest.targetId)
      .flatMap {
        case Some(application) =>
          val signature = application.contract.signatures
            .find(_.signatureName == jsonServeRequest.signatureName)
            .getOrElse(throw new IllegalArgumentException(s"Application ${jsonServeRequest.targetId} doesn't have a ${jsonServeRequest.signatureName} signature"))

          val ds = new SignatureBuilder(signature).convert(jsonServeRequest.inputs).right.map { tensors =>
            PredictRequest(
              modelSpec = Some(
                ModelSpec(
                  name = application.name,
                  signatureName = jsonServeRequest.signatureName,
                  version = None
                )
              ),
              inputs = tensors.mapValues(_.toProto)
            )
          }
          ds match {
            case Left(validationError) => Result.clientErrorF(s"Tensor validation errors: ${validationError.getMessage}")
            case Right(request) =>
              serveApplication(application, request).map { result =>
                result.right.map(responseToJsObject)
              }
          }
        case None =>
          Future.failed(new IllegalArgumentException(s"Can't find Application with id=${jsonServeRequest.targetId}"))
      }
  }

  def get(appId: Long): HFResult[Application] = {
    applicationRepository.get(appId).map {
      case Some(app) => Result.ok(app)
      case None => Result.clientError(s"Can't find application with ID $appId")
    }
  }

  def allApplications(): Future[Seq[Application]] = {
    applicationRepository.all().flatMap(enrichApplications)
  }

  def enrichApplication(app: Application): Future[Option[Application]] = {
    enrichApplications(Seq(app)).map(_.headOption)
  }

  def enrichApplication(app: Option[Application]): Future[Option[Application]] = {
    enrichApplications(app.toSeq).map(_.headOption)
  }


  def enrichApplications(apps: Seq[Application]): Future[Seq[Application]] = {

    def extractServiceField[E](extractor: ServiceKeyDescription => E): Seq[E] =
      for {
        app <- apps
        stage <- app.executionGraph.stages
        service <- stage.services
      } yield extractor(service.serviceDescription)


    def groupBy[K, V](seq: Seq[V])(idExtractor: V => K): Map[K, V] = seq
      .groupBy(idExtractor(_))
      .mapValues(_.headOption)
      .filter(_._2.isDefined)
      .mapValues(_.get)

    val modelIds = extractServiceField(_.modelVersionId).flatten

    val modelsAndVersionsById: FutureMap[ModelVersion] = modelVersionManagementService.modelVersionsByModelVersionIds(modelIds)
      .map(groupBy(_) {
        _.id
      })

    val runtimesById: FutureMap[Runtime] = runtimeRepository.all().map(groupBy(_) {
      _.id
    })

    enrichServiceKeyDescription(apps, runtimesById, modelsAndVersionsById)

  }

  def enrichServiceKeyDescription(apps: Seq[Application],
    futureRuntimes: FutureMap[Runtime],
    futureModels: FutureMap[ModelVersion]): Future[Seq[Application]] = {

    def enrichApps(apps: Seq[Application])
      (enrich: ServiceKeyDescription => ServiceKeyDescription): Seq[Application] = apps.map {
      app =>
        app.copy(
          executionGraph = app.executionGraph.copy(
            stages = app.executionGraph.stages.map {
              stage =>
                stage.copy(
                  services = stage.services.map {
                    service =>
                      service.copy(
                        serviceDescription = enrich(service.serviceDescription)
                      )
                  }
                )
            }
          )
        )
    }

    for {
      modelData <- futureModels
      runtimeData <- futureRuntimes
    } yield enrichApps(apps) {
      key =>
        key.copy(
          modelName = key.modelVersionId.flatMap(modelData.get(_).map(mv => s"${mv.modelName}:${mv.modelVersion}")),
          runtimeName = runtimeData.get(key.runtimeId).map(_.name)
        )
    }
  }


  def getApplication(id: Long): HFResult[Application] = {
    get(id).flatMap {
      case Left(err) => Result.errorF(err)
      case Right(app) =>
        enrichApplication(app).map(_.toHResult(ClientError("Can't enrich application")))
    }
  }


  def generateInputsForApplication(appId: Long, signatureName: String): HFResult[JsObject] = {
    get(appId).map { result =>
      result.right.flatMap { app =>
        app.contract.signatures.find(_.signatureName == signatureName) match {
          case Some(signature) =>
            val data = DataGenerator(signature).generateInputs
            Result.ok(TensorJsonLens.mapToJson(data))
          case None => Result.clientError(s"Can't find signature '$signatureName")
        }
      }
    }
  }

  def createApplication(req: CreateApplicationRequest): HFResult[Application] = executeWithSync {
    val keys = for {
      stage <- req.executionGraph.stages
      service <- stage.services
    } yield {
      service.toDescription
    }
    val keySet = keys.toSet

    serviceManagementService.fetchServicesUnsync(keySet).flatMap { services =>
      val existedServices = services.map(_.toServiceKeyDescription)
      startServices(keySet -- existedServices).flatMap { _ =>
        composeApp(req).flatMap {
          case Left(err) => Result.errorF(err)
          case Right(inferredApp) =>
            applicationRepository.create(inferredApp).flatMap { created =>
              enrichApplication(created).map {
                case Some(enriched) =>
                  internalManagerEventsPublisher.applicationChanged(created)
                  Result.ok(enriched)
                case None => Result.clientError("Can't enrich application")
              }
            }
        }
      }
    }
  }

  def deleteApplication(id: Long): HFResult[Application] =
    executeWithSync {
      get(id).flatMap {
        case Right(application) =>
          val keysSet = application.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet
          applicationRepository.delete(id)
            .flatMap { _ =>
              removeServiceIfNeeded(keysSet, id)
                .map { _ =>
                  internalManagerEventsPublisher.applicationRemoved(application)
                  Result.ok(application)
                }
            }
        case Left(error) =>
          Result.errorF(error)
      }
    }

  def updateApplication(req: UpdateApplicationRequest): HFResult[Application] = {
    executeWithSync {
      get(req.id)
        .flatMap {
          case Right(application) =>
            val keysSetOld = application.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet
            val keysSetNew = req.executionGraph.stages.flatMap(_.services.map(_.toDescription)).toSet

            removeServiceIfNeeded(keysSetOld -- keysSetNew, application.id).flatMap { _ =>
              startServices(keysSetNew -- keysSetOld).flatMap { _ =>
                composeApp(req).flatMap {
                  case Left(err) => Result.errorF(err)
                  case Right(app) =>
                    applicationRepository.update(app).flatMap { _ =>
                      enrichApplication(app).map {
                        case Some(enriched) =>
                          internalManagerEventsPublisher.applicationChanged(app)
                          Result.ok(enriched)
                        case None => Result.clientError("Can't enrich application")
                      }
                    }
                }
              }
            }
          case Left(error) => Result.errorF(error)
        }
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

  private def composeApp(id: Long = 0, name: String, executionGraph: ExecutionGraphRequest, kafkaStreaming: List[ApplicationKafkaStream]): HFResult[Application] = {
    inferGraph(executionGraph).flatMap {
      case Right(graph) =>
        inferAppContract(name, graph).map {
          case Right(contract) =>
            Result.ok(
              Application(
                id = id,
                name = name,
                contract = contract,
                executionGraph = graph,
                kafkaStreaming = kafkaStreaming
              )
            )
          case Left(err) => Result.error(err)
        }
      case Left(err) => Result.errorF(err)
    }
  }

  private def composeApp(appReq: UpdateApplicationRequest): HFResult[Application] = {
     composeApp(appReq.id, appReq.name, appReq.executionGraph,  appReq.kafkaStream.getOrElse(Seq.empty).toList)
  }

  private def composeApp(appReq: CreateApplicationRequest): HFResult[Application] = {
    composeApp(0, appReq.name, appReq.executionGraph,  appReq.kafkaStreaming)
  }

  private def inferGraph(executionGraphRequest: ExecutionGraphRequest): HFResult[ApplicationExecutionGraph] = {
    val appStages =
      executionGraphRequest.stages match {
        case singleStage :: Nil if singleStage.services.lengthCompare(1) == 0 =>
          Future.successful(inferSimpleApp(singleStage))
        case stages =>
          inferPipelineApp(stages)
      }
    appStages.map { result =>
      result.right.map { sigs =>
        ApplicationExecutionGraph(sigs.toList)
      }
    }
  }

  private def inferSimpleApp(singleStage: ExecutionStepRequest): HResult[Seq[ApplicationStage]] = {
    Result.ok(
      Seq(
        ApplicationStage(
          services = singleStage.services.map(_.toWeighedService.copy(weight = 100)),
          signature = None
        )
      )
    )
  }

  private def inferPipelineApp(stages: Seq[ExecutionStepRequest]): HFResult[Seq[ApplicationStage]] = {
    val fResult = Future.sequence {
      stages.zipWithIndex.map {
        case (stage, id) =>
          inferServices(stage.services).map {
            case Left(err) => Result.error(err)
            case Right(services) =>
              inferStageSignature(services).right.map { stageSigs =>
                ApplicationStage(
                  services = services.toList,
                  signature = Some(stageSigs.withSignatureName(id.toString))
                )
              }
          }
      }
    }

    fResult.map(Result.sequence)
  }

  def inferServices(services: List[SimpleServiceDescription]): HFResult[Seq[WeightedService]] = {
    val resultsF = Future.sequence {
      services.map { s =>
        s.modelVersionId match {
          case Some(vId) =>
            modelVersionManagementService.get(vId).map {
              case Right(version) =>
                val maybeSignature = version.modelContract.signatures
                  .find(_.signatureName == s.signatureName)
                maybeSignature match {
                  case Some(signature) =>
                    Result.ok(s.toWeighedService.copy(signature = Some(signature)))
                  case None => Result.clientError(s"$s doesn't contain such signature")
                }
              case Left(err) => Result.error(err)
            }
          case None => Result.clientErrorF(s"$s doesn't have a modelversion")
        }
      }
    }
    resultsF.map(Result.sequence)
  }

  private def inferAppContract(applicationName: String, graph: ApplicationExecutionGraph): HFResult[ModelContract] = {
    logger.debug(applicationName)
    graph.stages match {
      case stage :: Nil if stage.services.lengthCompare(1) == 0 => // single model version
        val serviceDesc = stage.services.head
        serviceManagementService.fetchServicesUnsync(Set(serviceDesc.serviceDescription)).map { services =>
          services.head.model match {
            case Some(model) => Result.ok(model.modelContract)
            case None => Result.clientError(s"Service $serviceDesc has no related model.")
          }
        }
      case _ =>
        Result.okF(
          ModelContract(
            applicationName,
            Seq(inferPipelineSignature(applicationName, graph))
          )
        )
    }
  }

  private def inferStageSignature(serviceDescs: Seq[WeightedService]): HResult[ModelSignature] = {
    val signatures = serviceDescs.map { service =>
      service.signature match {
        case Some(sig) => Result.ok(sig)
        case None => Result.clientError(s"$service doesn't have a signature")
      }
    }
    val errors = signatures.filter(_.isLeft).map(_.left.get)
    if (errors.nonEmpty) {
      Result.clientError(s"Errors while inferring stage signature: $errors")
    } else {
      val values = signatures.map(_.right.get)
      Result.ok(
        values.foldRight(ModelSignature.defaultInstance) {
          case (sig1, sig2) => ModelSignatureOps.merge(sig1, sig2)
        }
      )
    }
  }

  private def inferPipelineSignature(name: String, graph: ApplicationExecutionGraph): ModelSignature = {
    ModelSignature(
      name,
      graph.stages.head.signature.get.inputs,
      graph.stages.last.signature.get.outputs
    )
  }

  private def responseToJsObject(rr: PredictResponse): JsObject = {
    val fields = rr.outputs.mapValues(v => TensorJsonLens.toJson(TypedTensorFactory.create(v)))
    JsObject(fields)
  }
}

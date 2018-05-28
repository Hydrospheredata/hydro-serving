package io.hydrosphere.serving.manager.service.application

import java.util.concurrent.atomic.AtomicReference

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.DataGenerator
import io.hydrosphere.serving.contract.utils.ops.ModelSignatureOps
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, Header, Headers}
import io.hydrosphere.serving.manager.ApplicationConfig
import io.hydrosphere.serving.manager.controller.application._
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model.api.json.TensorJsonLens
import io.hydrosphere.serving.manager.model.api.tensor_builder.SignatureBuilder
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.model.{db, _}
import io.hydrosphere.serving.manager.repository.{ApplicationRepository, RuntimeRepository}
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.clouddriver.CloudDriverService
import io.hydrosphere.serving.manager.service.internal_events.InternalManagerEventsPublisher
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.service.service.{CreateServiceRequest, ServiceManagementService}
import io.hydrosphere.serving.monitoring.monitoring.ExecutionInformation.ResponseOrError
import io.hydrosphere.serving.monitoring.monitoring.{ExecutionError, ExecutionInformation, ExecutionMetadata, MonitoringServiceGrpc}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import io.hydrosphere.serving.tensorflow.tensor.{TensorProto, TypedTensorFactory}
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.apache.logging.log4j.scala.Logging
import spray.json.{JsObject, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

private case class ExecutionUnit(
  serviceName: String,
  servicePath: String,
  stageInfo: StageInfo
)

private case class StageInfo(
  applicationRequestId: String,
  signatureName: String,
  applicationId: Long,
  modelVersionId: Option[Long],
  stageId: String
)

class ApplicationManagementServiceImpl(
  applicationRepository: ApplicationRepository,
  modelVersionManagementService: ModelVersionManagementService,
  serviceManagementService: ServiceManagementService,
  grpcClient: PredictionServiceGrpc.PredictionServiceStub,
  grpcClientForMonitoring: MonitoringServiceGrpc.MonitoringServiceStub,
  internalManagerEventsPublisher: InternalManagerEventsPublisher,
  applicationConfig: ApplicationConfig,
  runtimeRepository: RuntimeRepository
)(implicit val ex: ExecutionContext) extends ApplicationManagementService with Logging {

  type FutureMap[T] = Future[Map[Long, T]]

  //TODO REMOVE!
  private def sendToDebug(responseOrError: ResponseOrError, predictRequest: PredictRequest, executionUnit: ExecutionUnit): Unit = {
    if (applicationConfig.shadowingOn) {
      val execInfo = ExecutionInformation(
        metadata = Option(ExecutionMetadata(
          applicationId = executionUnit.stageInfo.applicationId,
          stageId = executionUnit.stageInfo.stageId,
          modelVersionId = executionUnit.stageInfo.modelVersionId.getOrElse(-1),
          signatureName = executionUnit.stageInfo.signatureName,
          applicationRequestId = executionUnit.stageInfo.applicationRequestId,
          requestId = executionUnit.stageInfo.applicationRequestId //todo fetch from response
        )),
        request = Option(predictRequest),
        responseOrError = responseOrError
      )

      grpcClientForMonitoring
        .withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, CloudDriverService.GATEWAY_KAFKA_NAME)
        .withOption(Headers.XServingKafkaProduceTopic.callOptionsKey, "shadow_topic") //TODO where can i get this
        //.withOption(Headers.TraceId.callOptionsKey, executionUnit.stageInfo.applicationRequestId)
        .analyze(execInfo)
        .onComplete {
          case Failure(thr) =>
            logger.error("Can't send message to GATEWAY_KAFKA", thr)
          case _ =>
            Unit
        }
    }
  }

  private def getHeaderValue(header: Header): Option[String] = Option(header.contextKey.get())

  private def getCurrentExecutionUnit(unit: ExecutionUnit, modelVersionIdHeaderValue: AtomicReference[String]): ExecutionUnit = Try({
    Option(modelVersionIdHeaderValue.get()).map(_.toLong)
  }).map(s => unit.copy(stageInfo = unit.stageInfo.copy(modelVersionId = s)))
    .getOrElse(unit)


  private def getLatency(latencyHeaderValue: AtomicReference[String]): Try[TensorProto] = {
    Try({
      Option(latencyHeaderValue.get()).map(_.toLong)
    }).map(v => TensorProto(
      dtype = DataType.DT_INT64,
      int64Val = Seq(v.getOrElse(0)),
      tensorShape = Some(TensorShapeProto(dim = Seq(TensorShapeProto.Dim(1))))
    ))
  }


  def serve(unit: ExecutionUnit, request: PredictRequest): Future[PredictResponse] = {
    val modelVersionIdHeaderValue = new AtomicReference[String](null)
    val latencyHeaderValue = new AtomicReference[String](null)

    grpcClient
      .withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, unit.serviceName)
      .withOption(Headers.XServingModelVersionId.callOptionsClientResponseWrapperKey, modelVersionIdHeaderValue)
      .withOption(Headers.XEnvoyUpstreamServiceTime.callOptionsClientResponseWrapperKey, latencyHeaderValue)
      .predict(request)
      .transform(
        response => {
          val latency = getLatency(latencyHeaderValue)
          val res = if (latency.isSuccess) {
            response.addInternalInfo(
              "system.latency" -> latency.get
            )
          } else {
            response
          }

          sendToDebug(ResponseOrError.Response(res), request, getCurrentExecutionUnit(unit, modelVersionIdHeaderValue))
          response
        },
        thr => {
          logger.error("Can't send message to GATEWAY_KAFKA", thr)
          sendToDebug(ResponseOrError.Error(ExecutionError(thr.toString)), request, getCurrentExecutionUnit(unit, modelVersionIdHeaderValue))
          thr
        }
      )
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
            //TODO change to real ID
            val stageId = ApplicationStage.stageId(application.id, 0)
            val modelVersionId = application.executionGraph.stages.headOption.flatMap(
              _.services.headOption.flatMap(_.serviceDescription.modelVersionId))

            val stageInfo = StageInfo(
              modelVersionId = modelVersionId,
              applicationRequestId = "", // TODO get real traceId
              applicationId = application.id,
              signatureName = servicePath.signatureName,
              stageId = stageId
            )
            val unit = ExecutionUnit(
              serviceName = stageId,
              servicePath = servicePath.signatureName,
              stageInfo = stageInfo
            )
            serve(unit, request).map(Result.ok)
          case None => Result.clientErrorF("ModelSpec in request is not specified")
        }
      case stages => // pipeline
        val execUnits = stages.zipWithIndex.map {
          case (stage, idx) =>
            stage.signature match {
              case Some(signature) =>
                val stageInfo = StageInfo(
                  //TODO will be wrong modelVersionId during blue-green
                  //TODO Get this value from sidecar or in sidecar
                  modelVersionId = stage.services.headOption.flatMap(_.serviceDescription.modelVersionId),
                  applicationRequestId = "", // TODO get real traceId
                  applicationId = application.id,
                  signatureName = signature.signatureName,
                  stageId = ApplicationStage.stageId(application.id, idx)
                )
                Result.ok(
                  ExecutionUnit(
                    serviceName = ApplicationStage.stageId(application.id, idx),
                    servicePath = stage.services.head.signature.get.signatureName, // FIXME dirty hack to fix service signatures
                    stageInfo = stageInfo
                  )
                )
              case None => Result.clientError(s"$stage doesn't have a signature")
            }
        }

        Result.sequence(execUnits) match {
          case Left(err) => Result.errorF(err)
          case Right(units) =>
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
    getApplication(jsonServeRequest.targetId).flatMap {
      case Right(application) =>
        val signature = application.contract.signatures
          .find(_.signatureName == jsonServeRequest.signatureName)
          .toHResult(
            ClientError(s"Application ${jsonServeRequest.targetId} doesn't have a ${jsonServeRequest.signatureName} signature")
          )

        val ds = signature.right.map { sig =>
          new SignatureBuilder(sig).convert(jsonServeRequest.inputs).right.map { tensors =>
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
        }

        ds match {
          case Left(err) => Result.errorF(err)
          case Right(Left(tensorError)) => Result.clientErrorF(s"Tensor validation error: $tensorError")
          case Right(Right(request)) =>
            serveApplication(application, request).map { result =>
              result.right.map(responseToJsObject)
            }
        }
      case Left(error) =>
        Result.errorF(error)
    }
  }

  override def getApplication(appId: Long): HFResult[Application] = {
    applicationRepository.get(appId).flatMap {
      case Some(app) => enrichApplication(app)
      case None => Result.clientErrorF(s"Can't find application with ID $appId")
    }
  }

  def allApplications(): Future[Seq[Application]] = {
    applicationRepository.all().flatMap(enrichApplications)
  }

  def enrichApplication(app: Application): HFResult[Application] = {
    enrichApplications(Seq(app)).map(_.headOption.toHResult(ClientError("Can't enrich application")))
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

    val modelIds = extractServiceField(_.modelVersionId).flatten.toSet

    val modelsAndVersionsById: FutureMap[ModelVersion] = modelVersionManagementService.modelVersionsByModelVersionIds(modelIds)
      .map(groupBy(_) {
        _.id
      })

    val runtimesById: FutureMap[db.Runtime] = runtimeRepository.all().map(groupBy(_) {
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
          modelName = key.modelVersionId.flatMap(modelData.get(_).map(_.fullName)),
          runtimeName = runtimeData.get(key.runtimeId).map(_.toImageDef)
        )
    }
  }

  def generateInputsForApplication(appId: Long, signatureName: String): HFResult[JsObject] = {
    getApplication(appId).map { result =>
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

  def createApplication(
    name: String,
    executionGraph: ExecutionGraphRequest,
    kafkaStreaming: Seq[ApplicationKafkaStream]
  ): HFResult[Application] = executeWithSync {
    val keys = for {
      stage <- executionGraph.stages
      service <- stage.services
    } yield {
      service.toDescription
    }
    val keySet = keys.toSet

    val f = for {
      services <- EitherT(serviceManagementService.fetchServicesUnsync(keySet).map(Result.ok))
      existedServices = services.map(_.toServiceKeyDescription)
      _ <- EitherT(startServices(keySet -- existedServices))

      graph <- EitherT(inferGraph(executionGraph))
      contract <- EitherT(inferAppContract(name, graph))
      app <- EitherT(composeAppF(name, graph, contract, kafkaStreaming))

      createdApp <- EitherT(applicationRepository.create(app).map(Result.ok))
      enriched <- EitherT(enrichApplication(createdApp))
    } yield {
      internalManagerEventsPublisher.applicationChanged(createdApp)
      enriched
    }
    f.value
  }

  def deleteApplication(id: Long): HFResult[Application] =
    executeWithSync {
      getApplication(id).flatMap {
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

  def updateApplication(
    id: Long,
    name: String,
    executionGraph: ExecutionGraphRequest,
    kafkaStreaming: Seq[ApplicationKafkaStream]
  ): HFResult[Application] = {
    executeWithSync {
      val res = for {
        oldApplication <- EitherT(getApplication(id))

        keysSetOld = oldApplication.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet
        keysSetNew = executionGraph.stages.flatMap(_.services.map(_.toDescription)).toSet

        _ <- EitherT(removeServiceIfNeeded(keysSetOld -- keysSetNew, id))
        _ <- EitherT(startServices(keysSetNew -- keysSetOld))

        graph <- EitherT(inferGraph(executionGraph))
        contract <- EitherT(inferAppContract(name, graph))
        newApplication <- EitherT(composeAppF(name, graph, contract, kafkaStreaming, id))

        _ <- EitherT(applicationRepository.update(newApplication).map(Result.ok))
        enriched <- EitherT(enrichApplication(newApplication))
      } yield {
        internalManagerEventsPublisher.applicationChanged(enriched)
        enriched
      }
      res.value
    }
  }


  private def executeWithSync[A](func: => HFResult[A]): HFResult[A] = {
    applicationRepository.getLockForApplications().flatMap { lockInfo =>
      func andThen {
        case Success(r) =>
          applicationRepository.returnLockForApplications(lockInfo)
            .map(_ => r)
        case Failure(f) =>
          applicationRepository.returnLockForApplications(lockInfo)
            .map(_ => Result.internalError(f, "executeWithSync failed"))
      }
    }
  }

  private def startServices(keysSet: Set[ServiceKeyDescription]): HFResult[Seq[Service]] = {
    logger.debug(keysSet)
    serviceManagementService.fetchServicesUnsync(keysSet).flatMap { services =>
      val toAdd = keysSet -- services.map(_.toServiceKeyDescription)
      Result.traverseF(toAdd.toSeq) { key =>
        serviceManagementService.addService(
          CreateServiceRequest(
            serviceName = key.toServiceName(),
            runtimeId = key.runtimeId,
            configParams = None,
            environmentId = key.environmentId,
            modelVersionId = key.modelVersionId
          )
        )
      }
    }
  }

  private def removeServiceIfNeeded(keysSet: Set[ServiceKeyDescription], applicationId: Long): HFResult[Seq[Service]] = {
    val servicesF = for {
      apps <- applicationRepository.getKeysNotInApplication(keysSet, applicationId)
      keysSetOld = apps.flatMap(_.executionGraph.stages.flatMap(_.services.map(_.serviceDescription))).toSet
      services <- serviceManagementService.fetchServicesUnsync(keysSet -- keysSetOld)
    } yield services

    servicesF.flatMap { services =>
      Future.traverse(services) { service =>
        serviceManagementService.deleteService(service.id)
      }.map(Result.sequence)
    }
  }

  private def composeAppF(name: String, graph: ApplicationExecutionGraph, contract: ModelContract, kafkaStreaming: Seq[ApplicationKafkaStream], id: Long = 0) = {
    Result.okF(
      Application(
        id = id,
        name = name,
        contract = contract,
        executionGraph = graph,
        kafkaStreaming = kafkaStreaming.toList
      )
    )
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
        stage.services.headOption match {
          case Some(serviceDesc) =>
            serviceManagementService.fetchServicesUnsync(Set(serviceDesc.serviceDescription)).map { services =>
              services.headOption match {
                case Some(service) =>
                  service.model match {
                    case Some(model) => Result.ok(model.modelContract)
                    case None => Result.clientError(s"Service $serviceDesc has no related model.")
                  }
                case None => Result.clientError(s"Can't find '${serviceDesc.serviceDescription}' service information")
              }
            }
          case None => Result.clientErrorF(s"Can't infer contract for an empty stage")
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

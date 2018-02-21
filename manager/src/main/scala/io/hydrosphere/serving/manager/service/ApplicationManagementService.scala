package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.connector._
import io.hydrosphere.serving.manager.grpc.manager.AuthorityReplacerInterceptor
import io.hydrosphere.serving.manager.model.api.{DataGenerator, SignatureChecker}
import io.hydrosphere.serving.manager.model.api.ops.{ModelSignatureOps, TensorProtoOps}
import io.hydrosphere.serving.manager.model.api.validation.SignatureValidator
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.repository.ApplicationRepository
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import io.hydrosphere.serving.manager.controller.application.{CreateApplicationRequest, ExecutionGraphRequest, UpdateApplicationRequest}
import org.apache.logging.log4j.scala.Logging
import spray.json.{JsObject, JsValue}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class ServiceWithSignature(s: Service, signatureName: String)

case class ExecutionStep(
  serviceName: String,
  serviceSignature: String
)

trait ApplicationManagementService {
  def serveJsonApplication(jsonServeRequest: JsonServeRequest): Future[JsValue]

  def serveGrpcApplication(data: PredictRequest): Future[PredictResponse]

  def allApplications(): Future[Seq[Application]]

  def getApplication(id: Long): Future[Option[Application]]

  def generateInputsForApplication(appId: Long, signatureName: String): Future[Option[Seq[JsObject]]]

  def checkApplicationSchema(req: ApplicationExecutionGraph): Future[Boolean]

  def createApplication(req: CreateApplicationRequest): Future[Application]

  def deleteApplication(id: Long): Future[Unit]

  def updateApplication(req: UpdateApplicationRequest): Future[Application]
}

class ApplicationManagementServiceImpl(
  runtimeMeshConnector: RuntimeMeshConnector,
  applicationRepository: ApplicationRepository,
  serviceManagementService: ServiceManagementService,
  grpcClient: PredictionServiceGrpc.PredictionServiceStub,
  internalManagerEventsPublisher: InternalManagerEventsPublisher
)(implicit val ex: ExecutionContext) extends ApplicationManagementService with Logging {

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

  def allApplications(): Future[Seq[Application]] =
    applicationRepository.all()

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

  def checkApplicationSchema(graph: ApplicationExecutionGraph): Future[Boolean] = {
    val stages = graph.stages.zipWithIndex.map {
      case (stage, stIdx) =>
        val servicesId = stage.services.map(s => s.serviceDescription -> stage.signature.get.signatureName).toMap
        val services = servicesId.map {
          case (id, sig) =>
            //TODO change to batch fetch
            val modelService = Await.result(serviceManagementService.fetchServicesUnsync(Set(id)), 5.seconds)
              .headOption
              .getOrElse(throw new IllegalArgumentException(s"Service $id is not found."))
            modelService -> sig
        }.filterKeys(p => p.model.nonEmpty)
          .map { case (s, sig) => ServiceWithSignature(s, sig) }.toSeq
        createStageSignature(stIdx, services)
    }

    Future(
      stages.zip(stages.tail).forall {
        case (sig1, sig2) => SignatureChecker.areSequentiallyCompatible(sig1, sig2)
      }
    )
  }

  def createApplication(req: CreateApplicationRequest): Future[Application] = executeWithSync {
    val keys = for {
      stage <- req.executionGraph.stages
      service <- stage.services
    } yield {
      service.serviceDescription
    }
    val keySet = keys.toSet

    for {
      services <- serviceManagementService.fetchServicesUnsync(keySet)
      existedServices = services.map(_.toServiceKeyDescription)
      s <- startServices(keySet -- existedServices)
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
            val keysSetNew = req.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet

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

  private def inferGraph(executionGraphRequest: ExecutionGraphRequest) : Future[ApplicationExecutionGraph] = {
    val appStages =
      executionGraphRequest.stages match {
        case singleStage :: Nil if singleStage.services.lengthCompare(1) == 0 =>
          Future.successful {
            Seq(
              ApplicationStage(
                services = singleStage.services,
                signature = None
              )
            )
          }
        case stages =>
          Future.sequence {
            stages.map { stage =>
              inferStageSignature(stage.signatureName, stage.services.map(_.serviceDescription)).map { stageSig =>
                ApplicationStage(
                  services = stage.services,
                  signature = Some(stageSig)
                )
              }
            }
          }
      }

    appStages.map { sigs =>
      ApplicationExecutionGraph(sigs.toList)
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

  private def inferStageSignature(signatureName: String, stage: Seq[ServiceKeyDescription]): Future[ModelSignature] = {
    serviceManagementService.fetchServicesUnsync(stage.toSet).map { services =>
      services.map { service =>
        val m = service.model.getOrElse(throw new IllegalArgumentException(s"Service with id ${service.id} doesn't have a Model"))
        m.modelContract.signatures
          .find(_.signatureName == signatureName)
          .getOrElse(throw new IllegalArgumentException(s"Signature $signatureName for service ${service.id} is not found"))
      }
    }.map {
      _.foldRight(ModelSignature.defaultInstance) {
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

  private def createStageSignature(idx: Long, stages: Seq[ServiceWithSignature]): ModelSignature = {
    val signatures = stages.map { info =>
      info.s.model.getOrElse(throw new IllegalArgumentException(s"Can't find contract for service $info"))
        .modelContract.signatures
        .find(_.signatureName == info.signatureName)
        .getOrElse(throw new IllegalArgumentException(s"${info.signatureName} signature doesn't exist"))
    }
    signatures.fold(ModelSignature.defaultInstance)(ModelSignatureOps.merge)
  }
}

package io.hydrosphere.serving.manager.service

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
import org.apache.logging.log4j.scala.Logging
import spray.json.{JsObject, JsValue}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class ApplicationCreateOrUpdateRequest(
  id: Option[Long],
  name: String,
  executionGraph: ApplicationExecutionGraph
) {

  def toApplication: Application =
    Application(
      id = this.id.getOrElse(0),
      name = this.name,
      executionGraph = this.executionGraph
    )
}

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

  def generateInputsForApplication(appId: Long): Future[Option[Seq[JsObject]]]

  def checkApplicationSchema(req: ApplicationCreateOrUpdateRequest): Future[Boolean]

  def createApplications(req: ApplicationCreateOrUpdateRequest): Future[Application]

  def deleteApplication(id: Long): Future[Unit]

  def updateApplications(req: ApplicationCreateOrUpdateRequest): Future[Application]
}

class ApplicationManagementServiceImpl(
  runtimeMeshConnector: RuntimeMeshConnector,
  applicationRepository: ApplicationRepository,
  serviceManagementService: ServiceManagementService,
  grpcClient: PredictionServiceGrpc.PredictionServiceStub,
  internalManagerEventsPublisher: InternalManagerEventsPublisher
)(implicit val ex: ExecutionContext) extends ApplicationManagementService with ManagerJsonSupport with Logging {

  def createRequest(data: PredictResponse, signature: String): PredictRequest =
    PredictRequest(
      modelSpec = Some(
        ModelSpec(
          signatureName = signature
        )
      ),
      inputs = data.outputs
    )

  def serveGrpcStages(units: Seq[ExecutionUnit], data: PredictRequest): Future[PredictResponse] = {
    val empty = Future.successful(PredictResponse(outputs = data.inputs))

    units.foldLeft(empty) {
      case (a, b) =>
        a.flatMap { resp =>
          grpcClient
            .withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, b.serviceName)
            .predict(createRequest(resp, b.servicePath))
        }
    }
  }

  def serveGrpcApplication(data: PredictRequest): Future[PredictResponse] = {
    import ToPipelineStages._
    data.modelSpec match {
      case Some(modelSpec) =>
        applicationRepository.getByName(modelSpec.name).flatMap {
          case Some(app) =>
            serveGrpcStages(app.toPipelineStages(""), data)
          case None => Future.failed(new IllegalArgumentException(s"Application '${modelSpec.name}' is not found"))
        }
      case None => Future.failed(new IllegalArgumentException("ModelSpec is not defined"))
    }
  }

  override def serveJsonApplication(jsonServeRequest: JsonServeRequest): Future[JsValue] = {
    import ToPipelineStages._

    applicationRepository.get(jsonServeRequest.targetId)
      .flatMap({
        case Some(x) =>
          inferAppInputSchema(x).flatMap(signature => {
            val ds = new SignatureValidator(signature).convert(jsonServeRequest.inputs).right.map { tensors =>
              PredictRequest(
                modelSpec = None,
                inputs = tensors
              )
            }.right.map { grpcRequest => {
              serveGrpcStages(x.toPipelineStages(""), grpcRequest)
            }
            }

            ds match {
              case Left(l) =>
                Future.failed(new IllegalArgumentException(s"Can't map request $l"))
              case Right(r) =>
                r.map(TensorProtoOps.jsonify)
            }
          })
        case None =>
          Future.failed(new IllegalArgumentException(s"Can't find Application with id=${jsonServeRequest.targetId}"))
      })
  }

  override def allApplications(): Future[Seq[Application]] =
    applicationRepository.all()

  override def getApplication(id: Long): Future[Option[Application]] =
    applicationRepository.get(id)


  override def generateInputsForApplication(appId: Long): Future[Option[Seq[JsObject]]] = {
    applicationRepository.get(appId).flatMap {
      case Some(app) =>
        inferAppInputSchema(app).map { schema =>
          val data = DataGenerator(schema).generateInputs
          Some(Seq(TensorProtoOps.jsonify(data)))
        }
      case None =>
        Future.successful(None)
    }
  }

  private def inferAppInputSchema(application: Application): Future[ModelSignature] = {
    val stages = application.executionGraph.stages.map(st => st.services.map(s => s.serviceDescription -> st.signatureName).toMap)
    val headServices = Future.sequence {
      stages.head.map {
        case (sId, signature) =>
          serviceManagementService.fetchServicesUnsync(Set(sId)).map(maybeService => {
            val service = maybeService
              .headOption
              .getOrElse(throw new IllegalArgumentException(s"Service with id $sId is not found"))
            service.model.map(m => {
              m.modelContract.signatures
                .find(_.signatureName == signature)
                .getOrElse(throw new IllegalArgumentException(s"Signature $signature for service $sId is not found"))
            })
          })
      }.toList
    }
    headServices.map(services => {
      services
        .filter(_.nonEmpty)
        .map(_.get)
        .foldRight(ModelSignature()) {
          case (sig1, sig2) => ModelSignatureOps.merge(sig1, sig2)
        }
    })
  }


  override def checkApplicationSchema(req: ApplicationCreateOrUpdateRequest): Future[Boolean] = {
    val stages = req.executionGraph.stages.zipWithIndex.map {
      case (stage, stIdx) =>
        val servicesId = stage.services.map(s => s.serviceDescription -> stage.signatureName).toMap
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

  private def createStageSignature(idx: Long, stages: Seq[ServiceWithSignature]): ModelSignature = {
    val signatures = stages.map { info =>
      info.s.model.getOrElse(throw new IllegalArgumentException(s"Can't find contract for service $info"))
        .modelContract.signatures
        .find(_.signatureName == info.signatureName)
        .getOrElse(throw new IllegalArgumentException(s"${info.signatureName} signature doesn't exist"))
    }
    signatures.fold(ModelSignature.defaultInstance)(ModelSignatureOps.merge)
  }

  override def createApplications(req: ApplicationCreateOrUpdateRequest): Future[Application] = executeWithSync(() => {
    val keysSet = req.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet
    serviceManagementService.fetchServicesUnsync(keysSet).flatMap(s => {
      val existedServices = s.map(_.toServiceKeyDescription)

      val toAdd = keysSet -- existedServices
      startServices(toAdd).flatMap(_ => {
        applicationRepository.create(req.toApplication)
          .flatMap(a => {
            internalManagerEventsPublisher.applicationChanged(a)
            Future.successful(a)
          })
      })
    })
  })


  private def executeWithSync[A](func: () => Future[A]): Future[A] = {
    applicationRepository.getLockForApplications().flatMap(lockInfo => {
      func() andThen {
        case Success(r) =>
          applicationRepository.returnLockForApplications(lockInfo)
            .map(_ => r)
        case Failure(f) =>
          applicationRepository.returnLockForApplications(lockInfo)
            .map(_ => throw f)
      }
    })
  }


  private def startServices(keysSet: Set[ServiceKeyDescription]): Future[Seq[Service]] =
    serviceManagementService.fetchServicesUnsync(keysSet).flatMap(services => {
      val toAdd = keysSet -- services.map(_.toServiceKeyDescription)

      Future.traverse(toAdd.toSeq)(key => {
        serviceManagementService.addService(CreateServiceRequest(
          serviceName = key.toServiceName(),
          runtimeId = key.runtimeId,
          configParams = None,
          environmentId = key.environmentId,
          modelVersionId = key.modelVersionId
        ))
      })
    })

  private def removeServiceIfNeeded(keysSet: Set[ServiceKeyDescription], applicationId: Long): Future[Unit] =
    applicationRepository.getKeysNotInApplication(keysSet, applicationId)
      .flatMap(apps => {
        val keysSetOld = apps.flatMap(_.executionGraph.stages.flatMap(_.services.map(_.serviceDescription))).toSet
        serviceManagementService.fetchServicesUnsync(keysSet -- keysSetOld)
      })
      .flatMap(services => Future.traverse(services)(serv => {
        serviceManagementService.deleteService(serv.id)
      }).map(_ => Unit))


  override def deleteApplication(id: Long): Future[Unit] =
    executeWithSync(() => applicationRepository.get(id).flatMap {
      case Some(application) =>
        val keysSet = application.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet
        applicationRepository.delete(id)
          .flatMap(_ =>
            removeServiceIfNeeded(keysSet, id)
              .map(_ => internalManagerEventsPublisher.applicationRemoved(application))
          )
      case _ =>
        Future.successful(Unit)
    })

  def updateApplications(req: ApplicationCreateOrUpdateRequest): Future[Application] =
    executeWithSync(() => applicationRepository.get(req.id.getOrElse(throw new IllegalArgumentException(s"id required $req")))
      .flatMap {
        case Some(application) =>
          val keysSetOld = application.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet
          val keysSetNew = req.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet


          removeServiceIfNeeded(keysSetOld -- keysSetNew, application.id)
            .flatMap(_ =>
              startServices(keysSetNew -- keysSetOld).flatMap(_ => {
                val app = req.toApplication
                applicationRepository.update(app).map(_ => {
                  internalManagerEventsPublisher.applicationChanged(app)
                  app
                })
              })
            )
        case None =>
          throw new IllegalArgumentException(s"Can't find application $req")
      })


  /*

  override def serve(req: ServeRequest): Future[ExecutionResult] = {
    import ToPipelineStages._

    def buildStages[A](target: Option[A], error: => String)
      (implicit conv: ToPipelineStages[A]): Future[Seq[ExecutionUnit]] = {

      target match {
        case None => Future.failed(new IllegalArgumentException(error))
        case Some(a) => Future.successful(conv.toStages(a, req.servePath))
      }
    }

    val stagesFuture = req.serviceKey match {
      case ApplicationKey(id) =>
        val f = getApplication(id)
        f.flatMap(w => buildStages(w, s"Can't find Application with id: $id"))
      case ApplicationName(name) =>
        val f = applicationRepository.getByName(name)
        f.flatMap(w => buildStages(w, s"Can't find Application with name: $name"))
      case ModelById(id) =>
        val f = modelServiceRepository.get(id)
        f.flatMap(m => buildStages(m, s"Can't find model with id: $id"))
      case ModelByName(name, None) =>
        val f = modelServiceRepository.getLastModelServiceByModelName(name)
        f.flatMap(m => buildStages(m, s"Can find model with name: $name"))
      case ModelByName(name, Some(version)) =>
        val f = modelServiceRepository.getLastModelServiceByModelNameAndVersion(name, version)
        f.flatMap(m => buildStages(m, s"Can find model with name: $name, version: $version"))
    }

    stagesFuture.flatMap(stages => {
      val cmd = ExecutionCommand(
        headers = req.headers,
        json = req.inputData,
        pipe = stages
      )
      logger.info(s"TRY INVOKE $cmd")
      runtimeMeshConnector.execute(cmd)
    })
  }

  */
}

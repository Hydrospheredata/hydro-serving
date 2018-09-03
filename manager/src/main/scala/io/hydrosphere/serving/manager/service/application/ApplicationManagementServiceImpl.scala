package io.hydrosphere.serving.manager.service.application

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.config.ApplicationConfig
import io.hydrosphere.serving.manager.controller.application._
import io.hydrosphere.serving.model.api._
import io.hydrosphere.serving.model.api.Result.Implicits._
import io.hydrosphere.serving.model.api.json.TensorJsonLens
import io.hydrosphere.serving.model.api.ops.ModelSignatureOps
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.repository.ApplicationRepository
import io.hydrosphere.serving.manager.service.environment.{AnyEnvironment, EnvironmentManagementService}
import io.hydrosphere.serving.manager.service.internal_events.InternalManagerEventsPublisher
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.service.runtime.RuntimeManagementService
import io.hydrosphere.serving.manager.service.service.{CreateServiceRequest, ServiceManagementService}
import io.hydrosphere.serving.monitoring.monitoring.MonitoringServiceGrpc
import io.hydrosphere.serving.profiler.profiler.DataProfilerServiceGrpc
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

private case class ExecutionUnit(
  serviceName: String,
  servicePath: String,
  stageInfo: StageInfo,
)

private case class StageInfo(
  applicationRequestId: Option[String],
  signatureName: String,
  applicationId: Long,
  modelVersionId: Option[Long],
  stageId: String,
  applicationNamespace: Option[String],
  dataProfileFields: DataProfileFields = Map.empty
)

class ApplicationManagementServiceImpl(
  applicationRepository: ApplicationRepository,
  modelVersionManagementService: ModelVersionManagementService,
  environmentManagementService: EnvironmentManagementService,
  serviceManagementService: ServiceManagementService,
  internalManagerEventsPublisher: InternalManagerEventsPublisher,
  applicationConfig: ApplicationConfig,
  runtimeService: RuntimeManagementService
)(implicit val ex: ExecutionContext) extends ApplicationManagementService with Logging {

  type FutureMap[T] = Future[Map[Long, T]]

  override def getApplication(appId: Long): HFResult[Application] = {
    applicationRepository.get(appId).map {
      case Some(app) => Result.ok(app)
      case None => Result.clientError(s"Can't find application with ID $appId")
    }
  }

  def allApplications(): Future[Seq[Application]] = {
    applicationRepository.all()
  }

  def findVersionUsage(versionId: Long): Future[Seq[Application]] = {
    allApplications().map { apps =>
      apps.filter { app =>
        app.executionGraph.stages.exists { stage =>
          stage.services.exists { service =>
            service.serviceDescription.modelVersionId.contains(versionId)
          }
        }
      }
    }
  }

  def generateInputsForApplication(appId: Long, signatureName: String): HFResult[JsObject] = {
    getApplication(appId).map { result =>
      result.right.flatMap { app =>
        app.contract.signatures.find(_.signatureName == signatureName) match {
          case Some(signature) =>
            val data = TensorExampleGenerator(signature).inputs
            Result.ok(TensorJsonLens.mapToJson(data))
          case None => Result.clientError(s"Can't find signature '$signatureName")
        }
      }
    }
  }

  def createApplication(
    name: String,
    namespace: Option[String],
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
      graph <- EitherT(inferGraph(executionGraph))
      contract <- EitherT(inferAppContract(name, graph))
      app <- EitherT(composeAppF(name, namespace, graph, contract, kafkaStreaming))

      services <- EitherT(serviceManagementService.fetchServicesUnsync(keySet).map(Result.ok))
      existedServices = services.map(_.toServiceKeyDescription)
      _ <- EitherT(startServices(keySet -- existedServices))

      createdApp <- EitherT(applicationRepository.create(app).map(Result.ok))
    } yield {
      internalManagerEventsPublisher.applicationChanged(createdApp)
      createdApp
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
    namespace: Option[String],
    executionGraph: ExecutionGraphRequest,
    kafkaStreaming: Seq[ApplicationKafkaStream]
  ): HFResult[Application] = {
    executeWithSync {
      val res = for {
        oldApplication <- EitherT(getApplication(id))

        graph <- EitherT(inferGraph(executionGraph))
        contract <- EitherT(inferAppContract(name, graph))
        newApplication <- EitherT(composeAppF(name, namespace, graph, contract, kafkaStreaming, id))

        keysSetOld = oldApplication.executionGraph.stages.flatMap(_.services.map(_.serviceDescription)).toSet
        keysSetNew = executionGraph.stages.flatMap(_.services.map(_.toDescription)).toSet

        _ <- EitherT(removeServiceIfNeeded(keysSetOld -- keysSetNew, id))
        _ <- EitherT(startServices(keysSetNew -- keysSetOld))

        _ <- EitherT(applicationRepository.update(newApplication).map(Result.ok))
      } yield {
        internalManagerEventsPublisher.applicationChanged(newApplication)
        newApplication
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

  private def composeAppF(name: String, namespace: Option[String], graph: ApplicationExecutionGraph, contract: ModelContract, kafkaStreaming: Seq[ApplicationKafkaStream], id: Long = 0) = {
    Result.okF(
      Application(
        id = id,
        name = name,
        namespace = namespace,
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
          inferSimpleApp(singleStage) // don't perform checks
        case stages =>
          inferPipelineApp(stages)
      }
    EitherT(appStages).map { stages =>
      ApplicationExecutionGraph(stages.toList)
    }.value
  }

  private def inferSimpleApp(singleStage: ExecutionStepRequest): HFResult[Seq[ApplicationStage]] = {
    val service = singleStage.services.head
    service.modelVersionId match {
      case Some(vId) =>
        val f = for {
          version <- EitherT(modelVersionManagementService.get(vId))
          runtime <- EitherT(runtimeService.get(service.runtimeId))
          environment <- EitherT(environmentManagementService.get(service.environmentId.getOrElse(AnyEnvironment.id)))
          signed <- EitherT(createDetailedServiceDesc(service, version, runtime, environment, None))
        } yield Seq(
          ApplicationStage(
            services = List(signed.copy(weight = 100)), // 100 since this is the only service in the app
            signature = None,
            dataProfileFields = signed.modelVersion.dataProfileTypes.getOrElse(Map.empty)
          )
        )
        f.value
      case None => Result.clientErrorF(s"$service doesn't have a modelversion")
    }
  }

  private def inferPipelineApp(stages: Seq[ExecutionStepRequest]): HFResult[Seq[ApplicationStage]] = {
    Result.sequenceF {
      stages.zipWithIndex.map {
        case (stage, id) =>
          val f = for {
            services <- EitherT(inferServices(stage.services))
            stageSigs <- EitherT(Future.successful(inferStageSignature(services)))
          } yield {
            ApplicationStage(
              services = services.toList,
              signature = Some(stageSigs.withSignatureName(id.toString)),
              dataProfileFields = mergeServiceDataProfilingTypes(services)
            )
          }
          f.value
      }
    }
  }

  private def mergeServiceDataProfilingTypes(services: Seq[DetailedServiceDescription]): DataProfileFields = {
    val maps = services.map { s =>
      s.modelVersion.dataProfileTypes.getOrElse(Map.empty)
    }
    maps.reduce((a, b) => a ++ b)
  }

  def inferServices(services: List[ServiceCreationDescription]): HFResult[Seq[DetailedServiceDescription]] = {
    Result.sequenceF {
      services.map { service =>
        service.modelVersionId match {
          case Some(vId) =>
            val f = for {
              version <- EitherT(modelVersionManagementService.get(vId))
              runtime <- EitherT(runtimeService.get(service.runtimeId))
              signature <- EitherT(findSignature(version, service.signatureName))
              environment <- EitherT(environmentManagementService.get(service.environmentId.getOrElse(AnyEnvironment.id)))
              signed <- EitherT(createDetailedServiceDesc(service, version, runtime, environment, Some(signature)))
            } yield signed
            f.value
          case None => Result.clientErrorF(s"$service doesn't have a modelversion")
        }
      }
    }
  }

  private def findSignature(version: ModelVersion, signature: String) = {
    Future.successful {
      version.modelContract.signatures
        .find(_.signatureName == signature)
        .toHResult(Result.ClientError(s"Can't find signature $signature in $version"))
    }
  }

  private def createDetailedServiceDesc(service: ServiceCreationDescription, modelVersion: ModelVersion, runtime: Runtime, environment: Environment, signature: Option[ModelSignature]) = {
    Result.okF(
      DetailedServiceDescription(
        runtime,
        modelVersion,
        environment,
        service.weight,
        signature
      )
    )
  }

  private def inferAppContract(applicationName: String, graph: ApplicationExecutionGraph): HFResult[ModelContract] = {
    logger.debug(applicationName)
    graph.stages match {
      case stage :: Nil if stage.services.lengthCompare(1) == 0 => // single model version
        stage.services.headOption match {
          case Some(serviceDesc) =>
            Result.okF(serviceDesc.modelVersion.modelContract)
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

  private def inferStageSignature(serviceDescs: Seq[DetailedServiceDescription]): HResult[ModelSignature] = {
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
}
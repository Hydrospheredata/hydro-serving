package io.hydrosphere.serving.manager.domain.application

import cats.data.EitherT
import cats.instances.future._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.api.http.controller.application._
import io.hydrosphere.serving.manager.config.ApplicationConfig
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorService
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionService}
import io.hydrosphere.serving.manager.domain.service.{Service, ServiceManagementService}
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.InternalManagerEventsPublisher
import io.hydrosphere.serving.model.api.Result.Implicits._
import io.hydrosphere.serving.model.api._
import io.hydrosphere.serving.model.api.json.TensorJsonLens
import io.hydrosphere.serving.model.api.ops.ModelSignatureOps
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}

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
)

class ApplicationService(
  applicationRepository: ApplicationRepositoryAlgebra[Future],
  modelVersionManagementService: ModelVersionService,
  environmentManagementService: HostSelectorService,
  serviceManagementService: ServiceManagementService,
  internalManagerEventsPublisher: InternalManagerEventsPublisher,
  applicationConfig: ApplicationConfig
)(implicit val ex: ExecutionContext) extends Logging {

  type FutureMap[T] = Future[Map[Long, T]]

  def getApplication(appId: Long): HFResult[Application] = {
    applicationRepository.get(appId).map {
      case Some(app) => Result.ok(app)
      case None => Result.clientError(s"Can't find application with ID $appId")
    }
  }

  def getApplication(name: String): HFResult[Application] = {
    applicationRepository.get(name).map {
      case Some(app) => Result.ok(app)
      case None => Result.clientError(s"Can't find application with name $name")
    }
  }

  def allApplications(): Future[Seq[Application]] = {
    applicationRepository.all()
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
  ): HFResult[Application] = {
    val keys = for {
      stage <- executionGraph.stages
      service <- stage.services
    } yield {
      service.modelVersionId
    }
    val keySet = keys.toSet

    val f = for {
      graph <- EitherT(inferGraph(executionGraph))
      contract <- EitherT(inferAppContract(name, graph))
      app <- EitherT(composeAppF(name, namespace, graph, contract, kafkaStreaming))

      services <- EitherT(serviceManagementService.fetchServicesUnsync(keySet).map(Result.ok))
      existedServices = services.map(_.modelVersion.id)

      versions <- EitherT.liftF(modelVersionManagementService.get(keySet -- existedServices))
      _ <- EitherT(deployModelVersion(versions.toSet))

      createdApp <- EitherT(applicationRepository.create(app).map(Result.ok))
    } yield {
      internalManagerEventsPublisher.applicationChanged(createdApp)
      createdApp
    }
    f.value
  }

  def deleteApplication(id: Long): HFResult[Application] = {
    getApplication(id).flatMap {
      case Right(application) =>
        val keysSet = application.executionGraph.stages.flatMap(_.services.map(_.modelVersion.id)).toSet
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

  def deleteApplication(name: String): HFResult[Application] = {
    getApplication(name).flatMap {
      case Right(application) =>
        val keysSet = application.executionGraph.stages.flatMap(_.services.map(_.modelVersion.id)).toSet
        applicationRepository.delete(application.id)
          .flatMap { _ =>
            removeServiceIfNeeded(keysSet, application.id)
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
    val res = for {
      oldApplication <- EitherT(getApplication(id))

      graph <- EitherT(inferGraph(executionGraph))
      contract <- EitherT(inferAppContract(name, graph))
      newApplication <- EitherT(composeAppF(name, namespace, graph, contract, kafkaStreaming, id))

      keysSetOld = oldApplication.executionGraph.stages.flatMap(_.services.map(_.modelVersion.id)).toSet
      keysSetNew = executionGraph.stages.flatMap(_.services.map(_.modelVersionId)).toSet

      _ <- EitherT(removeServiceIfNeeded(keysSetOld -- keysSetNew, id))
      versions <- EitherT.liftF(modelVersionManagementService.get(keysSetNew -- keysSetOld))
      _ <- EitherT(deployModelVersion(versions.toSet))

      _ <- EitherT(applicationRepository.update(newApplication).map(Result.ok))
    } yield {
      internalManagerEventsPublisher.applicationChanged(newApplication)
      newApplication
    }
    res.value
  }

  private def deployModelVersion(modelVersions: Set[ModelVersion]): HFResult[Seq[Service]] = {
    Result.traverseF(modelVersions.toSeq) { mv =>
      serviceManagementService.addService(
        s"mv${mv.id}", None, mv
      )
    }
  }

  private def removeServiceIfNeeded(keysSet: Set[Long], applicationId: Long): HFResult[Seq[Service]] = {
    for {
      servicesToDelete <- retrieveRemovableServiceDescriptions(keysSet, applicationId)
      deleted <- removeServices(servicesToDelete)
    } yield deleted
  }

  private def retrieveRemovableServiceDescriptions(keysSet: Set[Long], applicationId: Long) = {
    for {
      apps <- applicationRepository.getKeysNotInApplication(keysSet, applicationId)
      keysSetOld = apps.flatMap(_.executionGraph.stages.flatMap(_.services.map(_.modelVersion.id))).toSet
      services <- serviceManagementService.fetchServicesUnsync(keysSet -- keysSetOld)
    } yield {
      logger.debug(s"applicationId=$applicationId keySet=$keysSet getKeysNotInApplication=${apps.map(_.name)} keysSetOld=$keysSetOld")
      services
    }
  }

  private def removeServices(services: Seq[Service]) = {
    logger.debug(s"Services to remove: ${services.map(_.serviceName)}")
    Result.traverseF(services) { service =>
      serviceManagementService.deleteService(service.id)
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
    val f = for {
      version <- EitherT(modelVersionManagementService.get(service.modelVersionId))
      signed <- EitherT(createDetailedServiceDesc(service, version, None))
    } yield Seq(
      ApplicationStage(
        services = List(signed.copy(weight = 100)), // 100 since this is the only service in the app
        signature = None,
      )
    )
    f.value
  }

  private def inferPipelineApp(stages: Seq[ExecutionStepRequest]): HFResult[Seq[ApplicationStage]] = {
    Result.sequenceF {
      stages.map { stage =>
          val f = for {
            services <- EitherT(inferServices(stage.services))
            stageSig <- EitherT(Future.successful(inferStageSignature(services)))
          } yield {
            ApplicationStage(
              services = services.toList,
              signature = Some(stageSig),
            )
          }
          f.value
      }
    }
  }

  def inferServices(services: List[ServiceCreationDescription]): HFResult[Seq[DetailedServiceDescription]] = {
    Result.sequenceF {
      services.map { service =>
        val f = for {
          version <- EitherT(modelVersionManagementService.get(service.modelVersionId))
          signature <- EitherT(findSignature(version, service.signatureName))
          signed <- EitherT(createDetailedServiceDesc(service, version, Some(signature)))
        } yield signed
        f.value
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

  private def createDetailedServiceDesc(service: ServiceCreationDescription, modelVersion: ModelVersion, signature: Option[ModelSignature]) = {
    Result.okF(
      DetailedServiceDescription(
        modelVersion,
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
      val signatureName = values.head.signatureName
      val isSameName = values.forall(_.signatureName == signatureName)
      if (isSameName) {
        Result.ok(
          values.foldRight(ModelSignature.defaultInstance) {
            case (sig1, sig2) => ModelSignatureOps.merge(sig1, sig2)
          }.withSignatureName(signatureName)
        )
      } else {
        Result.clientError(s"Model Versions ${serviceDescs.map(x => x.modelVersion.model.name + ":" + x.modelVersion.modelVersion)} have different signature names")
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
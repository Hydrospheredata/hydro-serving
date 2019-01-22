package io.hydrosphere.serving.manager.domain.application

import cats.data.EitherT
import cats.instances.future._
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.config.ApplicationConfig
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorService
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepositoryAlgebra}
import io.hydrosphere.serving.manager.domain.service.{Service, ServiceManagementService}
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.InternalManagerEventsPublisher
import io.hydrosphere.serving.model.api.Result.{ClientError, HError}
import io.hydrosphere.serving.model.api._
import io.hydrosphere.serving.model.api.json.TensorJsonLens
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}

class ApplicationService(
  applicationRepository: ApplicationRepositoryAlgebra[Future],
  versionRepository: ModelVersionRepositoryAlgebra[Future],
  environmentManagementService: HostSelectorService,
  serviceManagementService: ServiceManagementService,
  internalManagerEventsPublisher: InternalManagerEventsPublisher,
  applicationConfig: ApplicationConfig
)(implicit val ex: ExecutionContext) extends Logging {

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

  def generateInputs(name: String): HFResult[JsObject] = {
    val f = for {
      app <- EitherT(getApplication(name))
    } yield {
      val data = TensorExampleGenerator(app.signature).inputs
      TensorJsonLens.mapToJson(data)
    }
    f.value
  }

  def createApplication(
    name: String,
    namespace: Option[String],
    executionGraph: ExecutionGraphRequest,
    kafkaStreaming: Seq[ApplicationKafkaStream]
  ): HFResult[Application] = {
    val keys = for {
      stage <- executionGraph.stages
      service <- stage.modelVariants
    } yield {
      service.modelVersionId
    }
    val keySet = keys.toSet

    val f = for {
      _ <- checkApplicationName(name)
      graph <- EitherT(inferGraph(executionGraph))
      signature = ApplicationValidator.inferPipelineSignature(name, graph)
      app <- EitherT(composePendingApp(name, namespace, graph, signature, kafkaStreaming))

      services <- EitherT(serviceManagementService.fetchServicesUnsync(keySet).map(Result.ok))
      existedServices = services.map(_.modelVersion.id)
      serviceDiff = keySet -- existedServices
      versions <- EitherT.liftF(versionRepository.get(serviceDiff.toSeq))

      createdApp <- EitherT(applicationRepository.create(app).map(Result.ok))
    } yield {
      deployModelVersion(versions.toSet).map {
        case Left(err) =>
          logger.warn(s"ModelVersion deployment error: ${err.message}")
          val failedApp = createdApp.copy(status = ApplicationStatus.Failed)
          applicationRepository.update(failedApp)
        case Right(_) =>
          val finishedApp = createdApp.copy(status = ApplicationStatus.Ready)
          internalManagerEventsPublisher.applicationChanged(createdApp)
          applicationRepository.update(finishedApp)
      }.failed.foreach { x =>
        logger.warn(s"ModelVersion deployment exception", x)
        val failedApp = createdApp.copy(status = ApplicationStatus.Failed)
        applicationRepository.update(failedApp)
      }
      createdApp
    }
    f.value
  }

  def deleteApplication(id: Long): HFResult[Application] = {
    getApplication(id).flatMap {
      case Right(application) =>
        val keysSet = application.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion.id)).toSet
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
        val keysSet = application.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion.id)).toSet
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
      _ <- checkApplicationName(name)
      oldApplication <- EitherT(getApplication(id))

      graph <- EitherT(inferGraph(executionGraph))
      signature = ApplicationValidator.inferPipelineSignature(name, graph)
      newApplication <- EitherT(composePendingApp(name, namespace, graph, signature, kafkaStreaming, id))

      keysSetOld = oldApplication.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion.id)).toSet
      keysSetNew = executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersionId)).toSet
      servicesToAdd = keysSetNew -- keysSetOld
      servicesToRemove = keysSetOld -- keysSetNew

      _ <- EitherT(removeServiceIfNeeded(servicesToRemove, id))
      versions <- EitherT.liftF(versionRepository.get(servicesToAdd.toSeq))

      _ <- EitherT(applicationRepository.update(newApplication).map(Result.ok))
    } yield {
      deployModelVersion(versions.toSet).map {
        case Left(err) =>
          logger.warn(s"ModelVersion deployment error: ${err.message}")
          val failedApp = newApplication.copy(status = ApplicationStatus.Failed)
          internalManagerEventsPublisher.applicationRemoved(newApplication)
          applicationRepository.update(failedApp)
        case Right(_) =>
          val finishedApp = newApplication.copy(status = ApplicationStatus.Ready)
          internalManagerEventsPublisher.applicationChanged(newApplication)
          applicationRepository.update(finishedApp)
      }.failed.foreach { x =>
        logger.warn(s"ModelVersion deployment exception", x)
        val failedApp = newApplication.copy(status = ApplicationStatus.Failed)
        internalManagerEventsPublisher.applicationRemoved(newApplication)
        applicationRepository.update(failedApp)
      }
      newApplication
    }
    res.value
  }

  def checkApplicationName(name: String) = {
    for {
      _ <- EitherT.fromOption(ApplicationValidator.name(name), ClientError(s"Application name $name contains invalid symbols. It should only contain latin letters, numbers '-' and '_'"))
      _ <- EitherT.liftF[Future, HError, Unit](applicationRepository.get(name).map {
        case Some(_) => Result.clientError(s"Application with name $name already exists")
        case None => Right(())
      })
    } yield name
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
      keysSetOld = apps.flatMap(_.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion.id))).toSet
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


  private def composePendingApp(name: String, namespace: Option[String], graph: ApplicationExecutionGraph, signature: ModelSignature, kafkaStreaming: Seq[ApplicationKafkaStream], id: Long = 0) = {
    Result.okF(
      Application(
        id = id,
        name = name,
        namespace = namespace,
        signature = signature,
        executionGraph = graph,
        kafkaStreaming = kafkaStreaming.toList,
        status = ApplicationStatus.Pending
      )
    )
  }

  private def inferGraph(executionGraphRequest: ExecutionGraphRequest): HFResult[ApplicationExecutionGraph] = {
    val appStages =
      executionGraphRequest.stages match {
        case singleStage :: Nil if singleStage.modelVariants.lengthCompare(1) == 0 =>
          inferSimpleApp(singleStage) // don't perform checks
        case stages =>
          inferPipelineApp(stages)
      }
    EitherT(appStages).map { stages =>
      ApplicationExecutionGraph(stages.toList)
    }.value
  }

  private def inferSimpleApp(singleStage: PipelineStageRequest): HFResult[Seq[PipelineStage]] = {
    val service = singleStage.modelVariants.head
    val f = for {
      version <- EitherT.fromOptionF(versionRepository.get(service.modelVersionId), ClientError(s"Can't find model version with id ${service.modelVersionId}"))
      signature <- EitherT.fromOption(version.modelContract.signatures.find(_.signatureName == service.signatureName), ClientError(s"Can't find requested signature ${service.signatureName}"))
      signed <- EitherT(createDetailedServiceDesc(service, version, signature))
    } yield Seq(
      PipelineStage(
        modelVariants = List(signed.copy(weight = 100)), // 100 since this is the only service in the app
        signature = signature,
      )
    )
    f.value
  }

  private def inferPipelineApp(stages: Seq[PipelineStageRequest]): HFResult[Seq[PipelineStage]] = {
    Result.sequenceF {
      stages.map { stage =>
        val f = for {
          services <- EitherT(inferServices(stage.modelVariants))
          stageSig <- EitherT(Future.successful(ApplicationValidator.inferStageSignature(services)))
        } yield {
          PipelineStage(
            modelVariants = services.toList,
            signature = stageSig,
          )
        }
        f.value
      }
    }
  }

  def inferServices(services: Seq[ModelVariantRequest]): HFResult[Seq[ModelVariant]] = {
    Result.sequenceF {
      services.map { service =>
        val f = for {
          version <- EitherT.fromOptionF(versionRepository.get(service.modelVersionId), ClientError(s"Can't find model version with id ${service.modelVersionId}"))
          signature <- EitherT.fromOption(version.modelContract.signatures.find(_.signatureName == service.signatureName), ClientError(s"Can't find ${service.signatureName} signature"))
          signed <- EitherT(createDetailedServiceDesc(service, version, signature))
        } yield signed
        f.value
      }
    }
  }

  private def createDetailedServiceDesc(service: ModelVariantRequest, modelVersion: ModelVersion, signature: ModelSignature) = {
    Result.okF(
      ModelVariant(
        modelVersion,
        service.weight,
        signature
      )
    )
  }
}
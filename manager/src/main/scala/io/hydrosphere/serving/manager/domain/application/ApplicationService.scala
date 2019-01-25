package io.hydrosphere.serving.manager.domain.application

import cats.Traverse
import cats.data.EitherT
import cats.effect.{Effect, Sync}
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.domain.DomainError
import io.hydrosphere.serving.manager.domain.DomainError.{InvalidRequest, NotFound}
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepository}
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableRepository, ServableService}
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.ManagerEventBus
import io.hydrosphere.serving.model.api.TensorExampleGenerator
import io.hydrosphere.serving.model.api.json.TensorJsonLens
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.ExecutionContext
import scala.util.Try

trait ApplicationService[F[_]] {
  def generateInputs(name: String): F[Either[DomainError,JsObject]]

  def create(appRequest: CreateApplicationRequest): F[Either[DomainError, ApplicationBuildResult]]

  def delete(name: String): F[Either[DomainError,Application]]

  def update(appRequest: UpdateApplicationRequest): F[Either[DomainError, ApplicationBuildResult]]

  def checkApplicationName(name: String): F[Either[DomainError, String]]

  def get(name: String): F[Either[DomainError, Application]]
}

object ApplicationService {
  def apply[F[_] : Effect](
    applicationRepository: ApplicationRepository[F],
    versionRepository: ModelVersionRepository[F],
    servableService: ServableService[F],
    servableRepo: ServableRepository[F],
    internalManagerEventsPublisher: ManagerEventBus[F],
  )(implicit ex: ExecutionContext): ApplicationService[F] = new ApplicationService[F] with Logging {
    def generateInputs(name: String): F[Either[DomainError, JsObject]] = {
      val f = for {
        app <- EitherT(get(name))
        tensorData <- EitherT.fromOptionF(Sync[F].delay(Try(TensorExampleGenerator(app.signature).inputs).toOption), DomainError.internalError("Can't generate tensor example."))
        jsonData <- EitherT.fromOptionF[F, DomainError, JsObject](Sync[F].delay(Try(TensorJsonLens.mapToJson(tensorData)).toOption), DomainError.internalError("Can't convert"))
      } yield jsonData
      f.value
    }

    def create(appRequest: CreateApplicationRequest): F[Either[DomainError, ApplicationBuildResult]] = {
      val keys = for {
        stage <- appRequest.executionGraph.stages
        service <- stage.modelVariants
      } yield {
        service.modelVersionId
      }
      val keySet = keys.toSet

      val f = for {
        _ <- EitherT(checkApplicationName(appRequest.name))
        graph <- EitherT(inferGraph(appRequest.executionGraph))
        signature <- EitherT.fromOption(ApplicationValidator.inferPipelineSignature(appRequest.name, graph), DomainError.invalidRequest("Incompatible application stages"))(Effect[F])
        app = composeInitApp(appRequest.name, appRequest.namespace, graph, signature, appRequest.kafkaStreaming.getOrElse(List.empty))

        services <- EitherT.liftF[F, DomainError, Seq[Servable]](servableRepo.fetchByIds(keySet.toSeq))
        existedServices = services.map(_.modelVersion.id)
        serviceDiff = keySet -- existedServices
        versions <- EitherT.liftF[F, DomainError, Seq[ModelVersion]](versionRepository.get(serviceDiff.toSeq))

        createdApp <- EitherT.liftF[F, DomainError, Application](applicationRepository.create(app))
      } yield {
        val successfulApp = servableService.deployModelVersions(versions.toSet).map { _ =>
          val finishedApp = createdApp.copy(status = ApplicationStatus.Ready)
          internalManagerEventsPublisher.applicationChanged(createdApp)
          applicationRepository.update(finishedApp)
          finishedApp
        }
        val completedApp = Sync[F].onError(successfulApp) {
          case x => Effect[F].defer {
            logger.warn(s"ModelVersion deployment exception", x)
            val failedApp = createdApp.copy(status = ApplicationStatus.Failed)
            applicationRepository.update(failedApp).map(_ => ())
          }
        }

        ApplicationBuildResult(
          createdApp,
          Effect[F].toIO(completedApp).unsafeToFuture()
        )
      }
      f.value
    }

    def delete(name: String): F[Either[DomainError, Application]] = {
      val f = for {
        app <- EitherT(get(name))
        _ <- EitherT.liftF(applicationRepository.delete(app.id))
        keysSet = app.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion.id)).toSet
        _ <- EitherT.liftF[F, DomainError, Seq[Servable]](removeServiceIfNeeded(keysSet, app.id))
        _ <- EitherT.liftF[F, DomainError, Unit](internalManagerEventsPublisher.applicationRemoved(app))
      } yield app
      f.value
    }

    def update(appRequest: UpdateApplicationRequest): F[Either[DomainError, ApplicationBuildResult]] = {
      val res = for {
        _ <- EitherT(checkApplicationName(appRequest.name))
        oldApplication <- EitherT.fromOptionF(applicationRepository.get(appRequest.id), DomainError.notFound(s"Can't find application id ${appRequest.id}"))

        graph <- EitherT(inferGraph(appRequest.executionGraph))
        signature <- EitherT.fromOption(ApplicationValidator.inferPipelineSignature(appRequest.name, graph), DomainError.invalidRequest("Incompatible application stages"))(Sync[F])

        newApplication = composeInitApp(appRequest.name, appRequest.namespace, graph, signature, appRequest.kafkaStreaming.getOrElse(Seq.empty), appRequest.id)
        keysSetOld = oldApplication.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion.id)).toSet
        keysSetNew = appRequest.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersionId)).toSet
        servicesToAdd = keysSetNew -- keysSetOld
        servicesToRemove = keysSetOld -- keysSetNew

        _ <- EitherT.liftF[F, DomainError, Seq[Servable]](removeServiceIfNeeded(servicesToRemove, appRequest.id))
        versions <- EitherT.liftF[F, DomainError, Seq[ModelVersion]](versionRepository.get(servicesToAdd.toSeq))

        _ <- EitherT.liftF[F, DomainError, Int](applicationRepository.update(newApplication))
      } yield {
        val finishedApp = for {
          _ <- servableService.deployModelVersions(versions.toSet)
          finishedApp = newApplication.copy(status = ApplicationStatus.Ready)
          _ <- internalManagerEventsPublisher.applicationChanged(newApplication)
          _ <- applicationRepository.update(finishedApp)
        } yield finishedApp
        val completed = Sync[F].onError(finishedApp) {
          case x =>
            val failedApp = newApplication.copy(status = ApplicationStatus.Failed)
            for {
              _ <- internalManagerEventsPublisher.applicationRemoved(failedApp)
              _ <- applicationRepository.update(failedApp)
              _ <- Sync[F].delay(logger.warn(s"ModelVersion deployment exception", x))
            } yield ()
        }

        ApplicationBuildResult(
          newApplication,
          Effect[F].toIO(completed).unsafeToFuture()
        )
      }
      res.value
    }

    def checkApplicationName(name: String): F[Either[DomainError, String]] = {
      val f = for {
        _ <- EitherT.fromOption.apply[DomainError, String](
          ApplicationValidator.name(name), InvalidRequest(s"Application name $name contains invalid symbols. It should only contain latin letters, numbers '-' and '_'")
        )(Sync[F])
        _ <- EitherT.liftF[F, DomainError, Unit](applicationRepository.get(name).map {
          case Some(_) => Left(InvalidRequest(s"Application with name $name already exists"))
          case None => Right(())
        })
      } yield name
      f.value
    }

    private def removeServiceIfNeeded(keysSet: Set[Long], applicationId: Long): F[Seq[Servable]] = {
      for {
        servicesToDelete <- retrieveRemovableServiceDescriptions(keysSet, applicationId)
        deleted <- servableService.deleteServables(servicesToDelete.map(_.id))
      } yield deleted
    }

    private def retrieveRemovableServiceDescriptions(serviceKeys: Set[Long], applicationId: Long) = {
      for {
        apps <- applicationRepository.applicationsWithCommonServices(serviceKeys, applicationId)
        commonServiceKeys = apps.flatMap(_.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion.id))).toSet
        services <- servableRepo.fetchByIds((serviceKeys -- commonServiceKeys).toSeq)
      } yield {
        logger.debug(s"applicationId=$applicationId keySet=$serviceKeys getKeysNotInApplication=${apps.map(_.name)} keysSetOld=$commonServiceKeys")
        services.toList
      }
    }

    private def composeInitApp(name: String, namespace: Option[String], graph: ApplicationExecutionGraph, signature: ModelSignature, kafkaStreaming: Seq[ApplicationKafkaStream], id: Long = 0) = {
      Application(
        id = id,
        name = name,
        namespace = namespace,
        signature = signature,
        executionGraph = graph,
        kafkaStreaming = kafkaStreaming.toList,
        status = ApplicationStatus.Assembling
      )
    }

    private def inferGraph(executionGraphRequest: ExecutionGraphRequest): F[Either[DomainError, ApplicationExecutionGraph]] = {
      val appStages =
        executionGraphRequest.stages match {
          case singleStage :: Nil if singleStage.modelVariants.lengthCompare(1) == 0 =>
            inferSimpleApp(singleStage) // don't perform checks
          case stages =>
            inferPipelineApp(stages)
        }
      EitherT(appStages).map { stages =>
        ApplicationExecutionGraph(stages)
      }.value
    }

    private def inferSimpleApp(singleStage: PipelineStageRequest): F[Either[DomainError, List[PipelineStage]]] = {
      val service = singleStage.modelVariants.head
      val f = for {
        version <- EitherT.fromOptionF[F, DomainError, ModelVersion](versionRepository.get(service.modelVersionId), InvalidRequest(s"Can't find model version with id ${service.modelVersionId}"))
        signature <- EitherT.fromOption.apply[DomainError, ModelSignature](
          version.modelContract.signatures.find(_.signatureName == service.signatureName), InvalidRequest(s"Can't find requested signature ${service.signatureName}")
        )(Sync[F])
      } yield List(
        PipelineStage(
          modelVariants = List(ModelVariant(version, 100, signature)), // 100 since this is the only service in the app
          signature = signature,
        )
      )
      f.value
    }

    private def inferPipelineApp(stages: List[PipelineStageRequest]): F[Either[DomainError, List[PipelineStage]]] = {
      Traverse[List].traverse(stages) { stage =>
        for {
          services <- EitherT(inferServices(stage.modelVariants.toList))
          stageSig <- EitherT.fromEither(ApplicationValidator
            .inferStageSignature(services)
            .left
            .map(x => DomainError.invalidRequest(x.message))
          )(Sync[F])
        } yield {
          PipelineStage(
            modelVariants = services,
            signature = stageSig,
          )
        }
      }.value
    }

    private def inferServices(services: List[ModelVariantRequest]): F[Either[DomainError, List[ModelVariant]]] = {
      Traverse[List].traverse(services) { service =>
        for {
          version <- EitherT.fromOptionF(versionRepository.get(service.modelVersionId), DomainError.invalidRequest(s"Can't find model version with id ${service.modelVersionId}"))
          signature <- EitherT.fromOption.apply[DomainError, ModelSignature](
            version.modelContract.signatures.find(_.signatureName == service.signatureName), DomainError.invalidRequest(s"Can't find ${service.signatureName} signature")
          )(Sync[F])
        } yield ModelVariant(version, service.weight, signature)
      }.value
    }

    override def get(name: String): F[Either[DomainError, Application]] = {
      EitherT.fromOptionF[F, DomainError, Application](
        applicationRepository.get(name),
        NotFound(s"Application with name $name is not found")
      ).value
    }
  }
}
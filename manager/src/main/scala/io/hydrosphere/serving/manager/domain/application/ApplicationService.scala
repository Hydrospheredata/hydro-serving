package io.hydrosphere.serving.manager.domain.application

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Deferred
import cats.effect.implicits._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.discovery.DiscoveryHub
import io.hydrosphere.serving.manager.domain.DomainError
import io.hydrosphere.serving.manager.domain.DomainError.{InvalidRequest, NotFound}
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepository}
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableService, ServableStatus}
import io.hydrosphere.serving.manager.{domain, grpc}
import io.hydrosphere.serving.manager.grpc.entities.{ServingApp, Stage}
import io.hydrosphere.serving.model.api.TensorExampleGenerator
import io.hydrosphere.serving.model.api.json.TensorJsonLens
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.ExecutionContext
import scala.util.Try

trait ApplicationService[F[_]] {
  def generateInputs(name: String): F[Either[DomainError,JsObject]]

  def create(appRequest: CreateApplicationRequest): F[Either[DomainError, ApplicationBuildResult[F]]]

  def delete(name: String): F[Either[DomainError,Application]]

  def update(appRequest: UpdateApplicationRequest): F[Either[DomainError, ApplicationBuildResult[F]]]

  def checkApplicationName(name: String): F[Either[DomainError, String]]

  def get(name: String): F[Either[DomainError, Application]]
}

object ApplicationService {
  
  def apply[F[_] : Concurrent](
    applicationRepository: ApplicationRepository[F],
    versionRepository: ModelVersionRepository[F],
    servableService: ServableService[F],
    discoveryHub: DiscoveryHub[F]
  )(implicit ex: ExecutionContext): ApplicationService[F] = new ApplicationService[F] with Logging {
    
    def composeApp(
      name: String,
      namespace: Option[String],
      executionGraph: ExecutionGraphRequest,
      kafkaStreaming: Option[Seq[ApplicationKafkaStream]]
    ) = {
      for {
        _  <- EitherT(checkApplicationName(name))
        graph <- EitherT(inferGraph(executionGraph))
        signature <- EitherT.fromOption(ApplicationValidator.inferPipelineSignature(name, graph), DomainError.invalidRequest("Incompatible application stages"))(Concurrent[F])
      } yield composeInitApp(name, namespace, graph, signature, kafkaStreaming.getOrElse(List.empty))
    }

    def generateInputs(name: String): F[Either[DomainError, JsObject]] = {
      val f = for {
        app <- EitherT(get(name))
        tensorData <- EitherT.fromOptionF(Concurrent[F].delay(Try(TensorExampleGenerator(app.signature).inputs).toOption), DomainError.internalError("Can't generate tensor example."))
        jsonData <- EitherT.fromOptionF[F, DomainError, JsObject](Concurrent[F].delay(Try(TensorJsonLens.mapToJson(tensorData)).toOption), DomainError.internalError("Can't convert"))
      } yield jsonData
      f.value
    }
    
    private def startServices(application: Application, versions: Seq[ModelVersion]): F[Application] = {
      val finished = for {
        servables   <- versions.toList.traverse(mv => servableService.deploy(mv.servableName, mv.id, mv.image))
        finishedApp =  application.copy(status = ApplicationStatus.Ready)
        translated  <- Concurrent[F].fromEither(Internals.toServingApp(finishedApp, servables))
        _           <- discoveryHub.added(translated)
        _           <- applicationRepository.update(finishedApp)
      } yield finishedApp

      Concurrent[F].handleErrorWith(finished) { x =>
        for {
          _ <- Concurrent[F].delay(logger.error(s"ModelVersion deployment exception", x))
          failedApp = application.copy(status = ApplicationStatus.Failed)
          _ <- applicationRepository.update(failedApp)
        } yield failedApp
      }
    }

    def create(appRequest: CreateApplicationRequest): F[Either[DomainError, ApplicationBuildResult[F]]] = {
      val keys = for {
        stage <- appRequest.executionGraph.stages
        service <- stage.modelVariants
      } yield {
        service.modelVersionId
      }
      val keySet = keys.toSet

      val f = for {
        app <- composeApp(appRequest.name, appRequest.namespace, appRequest.executionGraph, appRequest.kafkaStreaming)
//        services <- EitherT.liftF[F, DomainError, Seq[Servable]](servableRepo.fetchByIds(keySet.toSeq))
//        existedServices = services.map(_.modelVersionId)
//        serviceDiff = keySet -- existedServices
        versions <- EitherT.liftF[F, DomainError, Seq[ModelVersion]](versionRepository.get(keySet.toSeq))

        createdApp <- EitherT.liftF[F, DomainError, Application](applicationRepository.create(app))
        df <- EitherT.liftF[F, DomainError, Deferred[F, Application]](Deferred[F, Application])
        _ <- EitherT.liftF[F, DomainError, Fiber[F, Unit]](startServices(createdApp, versions).flatMap(df.complete).start)
      } yield ApplicationBuildResult(createdApp, df)
      f.value
    }

    def delete(name: String): F[Either[DomainError, Application]] = {
      val f = for {
        app <- EitherT(get(name))
        _ <- EitherT.liftF(applicationRepository.delete(app.id))
        keysSet = app.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion)).toSet
        _ <- EitherT.liftF[F, DomainError, Unit](removeServiceIfNeeded(keysSet, app.id))
        _ <- EitherT.liftF[F, DomainError, Unit](discoveryHub.removed(app.id))
      } yield app
      f.value
    }

    def update(appRequest: UpdateApplicationRequest): F[Either[DomainError, ApplicationBuildResult[F]]] = {
      val res = for {
        oldApplication <- EitherT.fromOptionF(applicationRepository.get(appRequest.id), DomainError.notFound(s"Can't find application id ${appRequest.id}"))
        _ <- EitherT.liftF(discoveryHub.removed(oldApplication.id))

        composedApp <- composeApp(appRequest.name, appRequest.namespace, appRequest.executionGraph, appRequest.kafkaStreaming)
        newApplication = composedApp.copy(id = oldApplication.id)
        keysSetOld = oldApplication.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion)).toSet
        keysSetNew = appRequest.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersionId)).toSet
        servicesToAdd = keysSetNew.filterNot(id => keysSetOld.exists(_.id == id)) // -- keysSetOld
        servicesToRemove = keysSetOld.filterNot(mv => keysSetNew.contains(mv.id)) // -- keysSetNew

        _ <- EitherT.liftF[F, DomainError, Unit](removeServiceIfNeeded(servicesToRemove, appRequest.id))
        versions <- EitherT.liftF[F, DomainError, Seq[ModelVersion]](versionRepository.get(servicesToAdd.toSeq))

        _ <- EitherT.liftF[F, DomainError, Int](applicationRepository.update(newApplication))

        df <- EitherT.liftF[F, DomainError, Deferred[F, Application]](Deferred[F, Application])
        _ <- EitherT.liftF[F, DomainError, Fiber[F, Unit]](startServices(newApplication, versions).flatMap(df.complete).start)
      } yield ApplicationBuildResult(newApplication, df)
      res.value
    }

    def checkApplicationName(name: String): F[Either[DomainError, String]] = {
      val f = for {
        _ <- EitherT.fromOption.apply[DomainError, String](
          ApplicationValidator.name(name), InvalidRequest(s"Application name $name contains invalid symbols. It should only contain latin letters, numbers '-' and '_'")
        )(Concurrent[F])
        _ <- EitherT.liftF[F, DomainError, Unit](applicationRepository.get(name).map {
          case Some(_) => Left(InvalidRequest(s"Application with name $name already exists"))
          case None => Right(())
        })
      } yield name
      f.value
    }

    private def removeServiceIfNeeded(modelVersions: Set[ModelVersion], applicationId: Long): F[Unit] = {
      val keysSet = modelVersions.map(_.id)
      for {
        apps <- applicationRepository.applicationsWithCommonServices(keysSet, applicationId)
        commonServiceKeys = apps.flatMap(_.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion.id))).toSet
        servicesToDelete = modelVersions.filter(mv => !commonServiceKeys.contains(mv.id))
        _ <- servicesToDelete.toList.map(_.servableName).traverse(servableService.stop)
      } yield Unit
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
          version.modelContract.predict, InvalidRequest(s"Can't find predict signature")
        )(Concurrent[F])
      } yield List(
        PipelineStage(
          modelVariants = List(ModelVariant(version, 100)), // 100 since this is the only service in the app
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
          )(Concurrent[F])
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
        } yield ModelVariant(version, service.weight)
      }.value
    }

    override def get(name: String): F[Either[DomainError, Application]] = {
      EitherT.fromOptionF[F, DomainError, Application](
        applicationRepository.get(name),
        NotFound(s"Application with name $name is not found")
      ).value
    }
  }

  object Internals extends Logging {

    import io.hydrosphere.serving.manager.grpc.entities.{Servable => GServable}
    import io.hydrosphere.serving.manager.grpc.entities.{Stage => GStage}

    def toServingApp(
      app: Application,
      servables: List[Servable]
    ): Either[Throwable, ServingApp] = {
      toGStages(app, servables).map(stages => {
        val contract = ModelContract(
          modelName = app.name,
          predict = Some(app.signature)
        )
        
        ServingApp(
          app.id.toString,
          app.name,
          contract.some,
          stages
        )
      })
    }

    def modelVersionToGrpcEntity(mv: domain.model_version.ModelVersion): grpc.entities.ModelVersion = grpc.entities.ModelVersion(
      id = mv.id,
      version = mv.modelVersion,
      modelType = "",
      status = mv.status.toString,
      selector = mv.hostSelector.map(s => grpc.entities.HostSelector(s.id, s.name)),
      model = Some(grpc.entities.Model(mv.model.id, mv.model.name)),
      contract = Some(ModelContract(mv.modelContract.modelName, mv.modelContract.predict)),
      image = Some(grpc.entities.DockerImage(mv.image.name, mv.image.tag)),
      imageSha = mv.image.sha256.getOrElse(""),
      runtime = Some(grpc.entities.DockerImage(mv.runtime.name, mv.runtime.tag))
    )

    def toGServable(mv: ModelVariant, servables: Map[Long, Servable]): Either[Throwable, GServable] = {
      servables.get(mv.modelVersion.id) match {
        case Some(Servable(_, _, ServableStatus.Running(host, port))) => GServable(host, port, mv.weight, Some(modelVersionToGrpcEntity(mv.modelVersion))).asRight
        case Some(s) => new Exception(s"Invalid servable state for ${mv.modelVersion.model.name}:${mv.modelVersion.id} - $s").asLeft // TODO what about starting servables?
        case None => new Exception(s"Could not find servable for  ${mv.modelVersion.model.name}:${mv.modelVersion.id}").asLeft
      }
    }
  
    def toGStages(app: Application, servables: List[Servable]): Either[Throwable, List[GStage]] = {
      val asMap = servables.map(s => s.modelVersionId -> s).toMap
      app.executionGraph.stages.toList.zipWithIndex.traverse({
        case (st, i) =>
          val mapped = st.modelVariants.toList.traverse(mv => toGServable(mv, asMap))
          mapped.map(servables => GStage(i.toString, st.signature.some, servables))
      })
    }
  }
}
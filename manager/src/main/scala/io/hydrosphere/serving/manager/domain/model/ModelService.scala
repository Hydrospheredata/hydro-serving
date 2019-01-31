package io.hydrosphere.serving.manager.domain.model

import java.nio.file.Path

import cats.data.{EitherT, OptionT}
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.either._
import cats.{Monad, Traverse}
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.DomainError
import io.hydrosphere.serving.manager.domain.DomainError.{InvalidRequest, NotFound}
import io.hydrosphere.serving.manager.domain.application.{Application, ApplicationRepository}
import io.hydrosphere.serving.manager.domain.host_selector.{HostSelector, HostSelectorRepository}
import io.hydrosphere.serving.manager.domain.model_build.ModelVersionBuilder
import io.hydrosphere.serving.manager.domain.model_version.{BuildResult, ModelVersion, ModelVersionRepository, ModelVersionService}
import io.hydrosphere.serving.manager.infrastructure.storage.ModelStorage
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.ModelFetcher
import org.apache.logging.log4j.scala.Logging

trait ModelService[F[_]] {
  def get(modelId: Long): F[Either[DomainError, Model]]

  def upsertRequest(request: CreateModelRequest): F[Either[DomainError, Model]]

  def deleteModel(modelId: Long): F[Either[DomainError, Model]]

  def uploadModel(filePath: Path, meta: ModelUploadMetadata): F[Either[DomainError, BuildResult]]

  def checkIfUnique(targetModel: Model, newModelInfo: Model): F[Either[DomainError, Model]]

  def checkIfNoApps(versions: Seq[ModelVersion]): F[Either[DomainError, Unit]]
}

object ModelService {
  def apply[F[_] : Monad](
    modelRepository: ModelRepository[F],
    modelVersionService: ModelVersionService[F],
    modelVersionRepository: ModelVersionRepository[F],
    storageService: ModelStorage[F],
    appRepo: ApplicationRepository[F],
    hostSelectorRepository: HostSelectorRepository[F],
    fetcher: ModelFetcher[F],
    modelVersionBuilder: ModelVersionBuilder[F]
  ): ModelService[F] = new ModelService[F] with Logging {

    def deleteModel(modelId: Long): F[Either[DomainError, Model]] = {
      val f = for {
        model <- EitherT(get(modelId))
        versions <- EitherT.liftF[F, DomainError, Seq[ModelVersion]](modelVersionRepository.listForModel(model.id))
        _ <- EitherT(checkIfNoApps(versions))
        _ <- EitherT.liftF[F, DomainError, Seq[ModelVersion]](modelVersionService.deleteVersions(versions))
        _ <- EitherT.liftF[F, DomainError, Int](modelRepository.delete(model.id))
      } yield model
      f.value
    }

    def uploadModel(filePath: Path, meta: ModelUploadMetadata): F[Either[DomainError, BuildResult]] = {
      val maybeHostSelector = meta.hostSelectorName match {
        case Some(value) =>
          EitherT.fromOptionF(hostSelectorRepository.get(value), DomainError.invalidRequest(s"Can't find host selector named $value")).map(Option(_))
        case None => EitherT(Monad[F].pure(Either.right[DomainError, Option[HostSelector]](None)))
      }

      val f = for {
        hs <- maybeHostSelector
        modelPath <- EitherT.liftF[F, DomainError, Path](storageService.unpack(filePath, meta.name))
        contract <- OptionT.fromOption(meta.contract)
          .orElse(OptionT(fetcher.fetch(modelPath)).map(_.modelContract))
            .toRight(DomainError.invalidRequest("No contract provided and couldn't infer it from model files."))
        versionMetadata = ModelVersionMetadata.fromModel(contract, meta, hs)
        _ <- EitherT.fromEither(ModelVersionMetadata.validateContract(versionMetadata))
        request = CreateModelRequest(versionMetadata.modelName)
        req <- EitherT(upsertRequest(request))
        b <- EitherT.liftF[F, DomainError, BuildResult](modelVersionBuilder.build(req, versionMetadata))
      } yield b
      f.value
    }

    def upsertRequest(request: CreateModelRequest): F[Either[DomainError, Model]] = {
      modelRepository.get(request.name).flatMap {
        case Some(model) =>
          logger.info(s"Updating uploaded model with id: ${model.id} name: ${model.name}")
          val filledModel = model.copy(name = request.name)
          val f = for {
            _ <- EitherT(checkIfUnique(model, filledModel))
            _ <- EitherT.fromOptionF[F, DomainError, Path](storageService.rename(model.name, filledModel.name), DomainError.internalError("Couldn't find the model folder"))
            _ <- EitherT.liftF[F, DomainError, Int](modelRepository.update(filledModel))
          } yield filledModel
          f.value

        case None =>
          logger.info(s"Creating uploaded model with name: ${request.name}")
          EitherT.liftF[F, DomainError, Model](modelRepository.create(Model(-1, request.name))).value
      }
    }

    def checkIfUnique(targetModel: Model, newModelInfo: Model): F[Either[DomainError, Model]] = {
      modelRepository.get(newModelInfo.name).map {
        case Some(model) if model.id == targetModel.id => // it's the same model - ok
          Right(targetModel)

        case Some(model) => // it's other model - not ok
          val errMsg = InvalidRequest(s"There is already a model with same name: ${model.name}(${model.id}) -> ${newModelInfo.name}(${newModelInfo.id})")
          logger.error(errMsg)
          Left(errMsg)

        case None => // name is unique - ok
          Right(targetModel)
      }
    }

    def checkIfNoApps(versions: Seq[ModelVersion]): F[Either[DomainError, Unit]] = {
      implicit val traverse: Traverse[List] = cats.instances.list.catsStdInstancesForList

      def _checkApps(usedApps: Seq[Seq[Application]]): Either[DomainError, Unit] = {
        val allApps = usedApps.flatten.map(_.name)
        if (allApps.isEmpty) {
          Right(())
        } else {
          val appNames = allApps.mkString(", ")
          Left(DomainError.invalidRequest(s"Can't delete the model. It's used in [$appNames]."))
        }
      }

      val f = for {
        usedApps <- EitherT.liftF[F, DomainError, List[Seq[Application]]](
          Traverse[List].traverse(versions.map(_.id).toList)(appRepo.findVersionsUsage)
        )
        _ <- EitherT.fromEither(_checkApps(usedApps))
      } yield {}

      f.value
    }

    override def get(modelId: Long): F[Either[DomainError, Model]] = {
      EitherT.fromOptionF[F, DomainError, Model](
        modelRepository.get(modelId),
        NotFound(s"Can't find a model with id $modelId")
      ).value
    }
  }
}

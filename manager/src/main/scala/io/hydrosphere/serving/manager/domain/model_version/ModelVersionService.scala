package io.hydrosphere.serving.manager.domain.model_version

import cats.data.{EitherT, OptionT}
import cats.instances.list.catsStdInstancesForList
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Monad, Traverse}
import io.hydrosphere.serving.manager.domain.DomainError
import io.hydrosphere.serving.manager.domain.application.ApplicationRepository
import io.hydrosphere.serving.manager.domain.model.ModelValidator
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

trait ModelVersionService[F[_]] {
  def get(name: String, version: Long): F[Either[DomainError, ModelVersion]]

  def deleteVersions(mvs: Seq[ModelVersion]): F[Seq[ModelVersion]]

  def getNextModelVersion(modelId: Long): F[Long]

  def list: F[Seq[ModelVersionView]]

  def modelVersionsByModelVersionIds(modelIds: Set[Long]): F[Seq[ModelVersion]]

  def delete(versionId: Long): F[Option[ModelVersion]]
}

object ModelVersionService {
  def apply[F[_] : Monad](
    modelVersionRepository: ModelVersionRepository[F],
    applicationRepo: ApplicationRepository[F]
  )(implicit executionContext: ExecutionContext): ModelVersionService[F] = new ModelVersionService[F] with Logging {

    def deleteVersions(mvs: Seq[ModelVersion]): F[Seq[ModelVersion]] = {
      Traverse[List].traverse(mvs.toList) { version =>
        delete(version.id)
      }.map(_.flatten)
    }

    def list: F[Seq[ModelVersionView]] = {
      for {
        allVersions <- modelVersionRepository.all()
        f <- Traverse[List].traverse(allVersions.map(_.id).toList) { x =>
          applicationRepo.findVersionsUsage(x).map(x -> _)
        }
        usageMap = f.toMap
      } yield {
        allVersions.map { v =>
          ModelVersionView.fromVersion(v, usageMap.getOrElse(v.id, Seq.empty))
        }
      }
    }

    def modelVersionsByModelVersionIds(modelIds: Set[Long]): F[Seq[ModelVersion]] = {
      modelVersionRepository.modelVersionsByModelVersionIds(modelIds)
    }

    def delete(versionId: Long): F[Option[ModelVersion]] = {
      val f = for {
        version <- OptionT(modelVersionRepository.get(versionId))
        _ <- OptionT.liftF(modelVersionRepository.delete(versionId))
      } yield version
      f.value
    }

    def getNextModelVersion(modelId: Long): F[Long] = {
      for {
        versions <- modelVersionRepository.lastModelVersionByModel(modelId, 1)
      } yield versions.headOption.fold(1L)(_.modelVersion + 1)
    }

    override def get(name: String, version: Long): F[Either[DomainError, ModelVersion]] = {
      val f = for {
        _ <- EitherT.fromOption[F].apply(
          ModelValidator.name(name),
          DomainError.invalidRequest("Name contains invalid characters.")
        )
        version <- EitherT.fromOptionF(
          modelVersionRepository.get(name, version),
          DomainError.notFound(s"Can't find a ModelVersion $name:$version")
        )
      } yield version
      f.value
    }
  }
}
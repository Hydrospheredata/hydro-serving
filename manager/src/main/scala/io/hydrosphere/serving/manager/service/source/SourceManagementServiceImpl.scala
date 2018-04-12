package io.hydrosphere.serving.manager.service.source

import java.nio.file.Path

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.ManagerConfiguration
import io.hydrosphere.serving.manager.controller.model_source.{AddLocalSourceRequest, AddS3SourceRequest}
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelMetadata
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.{LocalSourceParams, S3SourceParams}
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.source.fetchers.ModelFetcher
import io.hydrosphere.serving.manager.service.source.sources.ModelSource
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class SourceManagementServiceImpl(
  managerConfiguration: ManagerConfiguration,
  sourceRepository: SourceConfigRepository)
  (implicit ex: ExecutionContext) extends SourceManagementService with Logging {

    def addSource(modelSourceConfigAux: ModelSourceConfig): HFResult[ModelSourceConfig] = {
      getSourceConfig(modelSourceConfigAux.name).flatMap {
        case Right(_) => Result.clientErrorF(s"ModelSource with name ${modelSourceConfigAux.name} already exists")
        case Left(ClientError(_)) => // consider more specific NotFound error?
          val modelSource = ModelSource.fromConfig(modelSourceConfigAux)
          for {
            config <- sourceRepository.create(modelSourceConfigAux)
          } yield {
            Right(config)
          }
        case Left(x) => Result.errorF(x)
      }
    }

    def getSourceConfig(name: String): HFResult[ModelSourceConfig] = {
      allSourceConfigs.map { sources =>
        sources.find(_.name == name)
          .map(Right.apply)
          .getOrElse(Result.clientError(s"Can't find a '$name' source"))
      }
    }

    override def getSources: Future[List[ModelSource]] = {
      allSourceConfigs.map { sources =>
        sources.map(ModelSource.fromConfig).toList
      }
    }

    override def getLocalPath(url: String): HFResult[Path] = {
      val args = url.split(':')
      val source = args.head
      val path = args.last
      getSources.map {
        _.find(_.sourceDef.name == source)
          .map(_.getAbsolutePath(path))
          .map(Right.apply)
          .getOrElse(Result.clientError(s"ModelSource for $url with prefix $source is not found"))
      }
    }

    override def addLocalSource(r: AddLocalSourceRequest): HFResult[ModelSourceConfig] = {
      val config = ModelSourceConfig(
        id = -1,
        name = r.name,
        params = LocalSourceParams(Some(r.path))
      )
      addSource(config)
    }

    override def addS3Source(r: AddS3SourceRequest): HFResult[ModelSourceConfig] = {
      val config = ModelSourceConfig(
        id = -1,
        name = r.name,
        params = S3SourceParams(
          awsAuth = r.key,
          bucketName = r.bucket,
          path = r.path,
          region = r.region
        )
      )
      addSource(config)
    }

    override def allSourceConfigs: Future[Seq[ModelSourceConfig]] = {
      sourceRepository.all().map { dbSources =>
        managerConfiguration.modelSources ++ dbSources
      }
    }

    override def getSource(name: String): HFResult[ModelSource] = {
      getSourceConfig(name).map { res =>
        res.right.map { config =>
          ModelSource.fromConfig(config)
        }
      }
    }

    override def index(modelSource: String): HFResult[Option[ModelMetadata]] = {
      val f = for {
        sourcePath <- EitherT(Future.successful(SourcePath.parse(modelSource)))
        source <- EitherT(getSource(sourcePath.sourceName))
      } yield {
        println(sourcePath)
        if (source.isExist(sourcePath.path)) {
          Some(ModelFetcher.fetch(source, sourcePath.path))
        } else {
          None
        }
      }
      f.value
    }

  }

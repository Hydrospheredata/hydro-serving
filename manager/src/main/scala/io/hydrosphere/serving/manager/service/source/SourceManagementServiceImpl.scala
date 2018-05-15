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

import io.hydrosphere.serving.manager.service.source.fetchers.ModelFetcher
import io.hydrosphere.serving.manager.service.source.sources.ModelSource
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class SourceManagementServiceImpl(
  managerConfiguration: ManagerConfiguration)
  (implicit ex: ExecutionContext) extends SourceManagementService with Logging {

  override def getLocalPath(sourcePath: SourcePath): HFResult[Path] = {
      val f = for {
        s <- EitherT(getSource(sourcePath.sourceName))
        file <- EitherT(Future.successful(s.getReadableFile(sourcePath.path)))
      } yield file.toPath
      f.value
    }

    override def index(modelSource: String): HFResult[Option[ModelMetadata]] = {
      val f = for {
        sourcePath <- EitherT(Future.successful(SourcePath.parse(modelSource)))
        source <- EitherT(getSource(sourcePath.sourceName))
      } yield {
        println(sourcePath)
        if (source.exists(sourcePath.path)) {
          Some(ModelFetcher.fetch(source, sourcePath.path))
        } else {
          None
        }
      }
      f.value
    }

  }

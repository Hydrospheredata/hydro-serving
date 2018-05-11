package io.hydrosphere.serving.manager.service.source

import java.nio.file.{Files, Path, Paths}

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.ManagerConfiguration
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.controller.model_source.{AddLocalSourceRequest, AddS3SourceRequest}
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.{ModelMetadata, ModelType}
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.{LocalSourceParams, S3SourceParams}
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.source.fetchers.ModelFetcher
import io.hydrosphere.serving.manager.service.source.sources.ModelSource
import io.hydrosphere.serving.manager.util.TarGzUtils
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ModelStorageManagementServiceImpl(
  managerConfiguration: ManagerConfiguration,
  sourceRepository: SourceConfigRepository)
  (implicit ex: ExecutionContext) extends ModelStorageManagementService with Logging {

  def upload(upload: ModelUpload): HFResult[SourceUploadResult] = {
    val fMaybeSource = upload.source match {
      case Some(sourceName) => getSource(sourceName)
      case None => getSources.map(_.headOption.toHResult(ClientError("No sources available")))
    }
    fMaybeSource.map { result =>
      result.right.map { source =>
        val uploadName = upload.tarballPath.getFileName.toString
        val unpackDir = Files.createTempDirectory(uploadName)
        val rootDir = Paths.get(uploadName)
        val uploadedFiles = TarGzUtils.decompress(upload.tarballPath, unpackDir)
        val localFiles = uploadedFiles
          .filter(_.startsWith(unpackDir))
          .map { path =>
            val relPath = unpackDir.relativize(path)
            path -> rootDir.resolve(relPath)
          }
          .toMap

        writeFilesToSource(source, localFiles)

        val inferredMeta = ModelFetcher.fetch(source, unpackDir.toString)
        val contract = upload.contract.getOrElse(inferredMeta.contract)
        val modelType = upload.modelType.map(ModelType.fromTag).getOrElse(inferredMeta.modelType)
        val modelName = upload.name.getOrElse(inferredMeta.modelName)

        SourceUploadResult(
          name = modelName,
          source = source.sourceDef.name,
          modelType = modelType,
          description = upload.description,
          modelContract = contract
        )
      }
    }
  }

  // TODO remove
  def addSource(modelSourceConfigAux: ModelSourceConfig): HFResult[ModelSourceConfig] = {
    getSourceConfig(modelSourceConfigAux.name).flatMap {
      case Right(_) => Result.clientErrorF(s"ModelSource with name ${modelSourceConfigAux.name} already exists")
      case Left(ClientError(_)) => // consider more specific NotFound error?
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

  override def getLocalPath(sourcePath: SourcePath): HFResult[Path] = {
    val f = for {
      s <- EitherT(getSource(sourcePath.sourceName))
      file <- EitherT(Future.successful(s.getReadableFile(sourcePath.path)))
    } yield file.toPath
    f.value
  }

  // TODO remove
  override def addLocalSource(r: AddLocalSourceRequest): HFResult[ModelSourceConfig] = {
    val config = ModelSourceConfig(
      id = -1,
      name = r.name,
      params = LocalSourceParams(Some(r.path))
    )
    addSource(config)
  }

  // TODO remove
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

  // TODO remove db
  override def allSourceConfigs: Future[Seq[ModelSourceConfig]] = {
    sourceRepository.all().map { dbSources =>
      managerConfiguration.modelSources ++ dbSources
    }
  }

  override def getSource(name: String): HFResult[ModelSource] = {
    EitherT(getSourceConfig(name)).map(ModelSource.fromConfig).value
  }

  // TODO remove
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

  private def writeFilesToSource(source: ModelSource, files: Map[Path, Path]): Unit = {
    files.foreach {
      case (src, dest) =>
        source.writeFile(dest.toString, src.toFile)
    }
  }

}

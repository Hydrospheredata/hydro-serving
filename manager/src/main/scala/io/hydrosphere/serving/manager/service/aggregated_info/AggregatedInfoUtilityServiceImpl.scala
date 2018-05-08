package io.hydrosphere.serving.manager.service.aggregated_info

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{Application, Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_build.ModelBuildManagmentService
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService

import scala.concurrent.{ExecutionContext, Future}

class AggregatedInfoUtilityServiceImpl(
  modelManagementService: ModelManagementService,
  modelBuildManagementService: ModelBuildManagmentService,
  modelVersionManagementService: ModelVersionManagementService,
  applicationManagementService: ApplicationManagementService
)(
  implicit executionContext: ExecutionContext
) extends AggregatedInfoUtilityService {

  override def allModelsAggregatedInfo(): Future[Seq[AggregatedModelInfo]] = {
    modelManagementService.allModels().flatMap(aggregatedInfo)
  }

  override def getModelAggregatedInfo(id: Long): HFResult[AggregatedModelInfo] = {
    val f = for {
      model <- EitherT(modelManagementService.getModel(id))
      info <- EitherT(aggregatedInfo(model))
    } yield info
    f.value
  }

  override def allModelVersions: Future[Seq[AggregatedModelVersion]] = {
    for {
      apps <- applicationManagementService.allApplications()
      versions <- modelVersionManagementService.list
    } yield {
      versions.map { version =>
        AggregatedModelVersion.fromModelVersion(version, findAppsUsage(version, apps))
      }
    }
  }


  override def getModelBuilds(modelId: Long): Future[Seq[AggregatedModelBuild]] = {
    for {
      apps <- applicationManagementService.allApplications()
      builds <- modelBuildManagementService.modelBuildsByModelId(modelId)
    } yield {
      builds.map { build =>
        AggregatedModelBuild.fromModelBuild(build, build.modelVersion.map(findAppsUsage(_, apps)).getOrElse(Seq.empty))
      }
    }
  }

  private def findAppsUsage(version: ModelVersion, apps: Seq[Application]): Seq[Application] = {
    apps.filter { app =>
      app.executionGraph.stages.exists { stage =>
        stage.services.exists { service =>
          service.serviceDescription.modelVersionId.contains(version.id)
        }
      }
    }
  }

  private def aggregatedInfo(models: Seq[Model]): Future[Seq[AggregatedModelInfo]] = {
    val ids = models.map(_.id)
    for {
      builds <- modelBuildManagementService.lastForModels(ids)
      buildsMap = builds.groupBy(_.model.id)
      versions <- modelVersionManagementService.lastModelVersionForModels(ids)
      versionsMap = versions.groupBy(_.model.get.id)
    } yield {
      models.map { model =>
        val lastVersion = versionsMap.get(model.id).map(_.maxBy(_.modelVersion))
        val lastBuild = buildsMap.get(model.id).map(_.maxBy(_.version))
        AggregatedModelInfo(
          model = model,
          lastModelBuild = lastBuild,
          lastModelVersion = lastVersion,
          nextVersion = getNextVersion(model, lastVersion)
        )
      }
    }
  }

  private def aggregatedInfo(model: Model): HFResult[AggregatedModelInfo] = {
    aggregatedInfo(Seq(model)).map(_.headOption).map {
      case Some(info) => Result.ok(info)
      case None => Result.clientError(s"Can't find aggregated info for model '${model.name}'")
    }
  }

  private def getNextVersion(model: Model, modelVersion: Option[ModelVersion]): Option[Long] = {
    modelVersion match {
      case Some(version) =>
        if (model.updated.isAfter(version.created)) {
          Some(version.modelVersion + 1)
        } else {
          None
        }
      case None =>
        Some(1L)
    }
  }
}

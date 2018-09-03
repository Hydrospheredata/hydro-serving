package io.hydrosphere.serving.manager.service.aggregated_info

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.model.db.{Application, Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_build.ModelBuildManagmentService
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.model.api.Result.HError
import io.hydrosphere.serving.model.api.{HFResult, Result}

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
    modelVersionManagementService.list.flatMap { versions =>
      Future.traverse(versions) { version =>
        applicationManagementService.findVersionUsage(version.id).map { apps =>
          AggregatedModelVersion.fromModelVersion(version, apps)
        }
      }
    }
  }

  override def getModelBuilds(modelId: Long): Future[Seq[AggregatedModelBuild]] = {
    modelBuildManagementService.modelBuildsByModelId(modelId).flatMap { builds =>
      Future.traverse(builds) { build =>
        val appsF = build.modelVersion.map { version =>
          applicationManagementService.findVersionUsage(version.id)
        }.getOrElse(Future.successful(Seq.empty))

        appsF.map { apps =>
          AggregatedModelBuild.fromModelBuild(build, apps)
        }
      }
    }
  }

  override def deleteModel(modelId: Long): HFResult[Model] = {
    val f = for {
      model <- EitherT(modelManagementService.getModel(modelId))
      versions <- EitherT(modelVersionManagementService.listForModel(model.id))
      _ <- EitherT(checkIfNoApps(versions))
      builds <- EitherT(modelBuildManagementService.listForModel(model.id))
      _ <- EitherT(deleteBuilds(builds))
      _ <- EitherT(deleteVersions(versions))
      _ <- EitherT(modelManagementService.delete(model.id))
    } yield {
      model
    }
    f.value
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

  private def deleteBuilds(builds: Seq[ModelBuild]): HFResult[Seq[ModelBuild]] = {
    Result.traverseF(builds) { build =>
      modelBuildManagementService.delete(build.id)
    }
  }

  private def deleteVersions(versions: Seq[ModelVersion]): HFResult[Seq[ModelVersion]] = {
    Result.traverseF(versions) { version =>
      modelVersionManagementService.delete(version.id)
    }
  }

  private def checkIfNoApps(versions: Seq[ModelVersion]): HFResult[Unit] = {
    def _checkApps(usedApps: Seq[Seq[Application]]): HFResult[Unit] = {
      val allApps = usedApps.flatten.map(_.name)
      if (allApps.isEmpty) {
        Result.okF(Unit)
      } else {
        val appNames = allApps.mkString(", ")
        Result.clientErrorF(s"Can't delete the model. It's used in [$appNames].")
      }
    }

    val f = for {
      usedApps <- EitherT.liftF[Future, HError, Seq[Seq[Application]]](Future.traverse(versions.map(_.id))(applicationManagementService.findVersionUsage))
      _ <- EitherT(_checkApps(usedApps))
    } yield {}

    f.value
  }
}

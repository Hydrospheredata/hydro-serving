package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{Model, ModelBuild, ModelVersion}

import scala.concurrent.{ExecutionContext, Future}

case class AggregatedModelInfo(
  model: Model,
  lastModelBuild: Option[ModelBuild],
  lastModelVersion: Option[ModelVersion],
  nextVersion: Option[Long]
)

trait AggregatedInfoUtilityService {
  def allModelsAggregatedInfo(): Future[Seq[AggregatedModelInfo]]

  def getModelAggregatedInfo(id: Long): HFResult[AggregatedModelInfo]
}

class AggregatedInfoUtilityServiceImpl(
  modelManagementService: ModelManagementService,
  modelBuildManagmentService: ModelBuildManagmentService,
  modelVersionManagementService: ModelVersionManagementService
)(
  implicit executionContext: ExecutionContext
) extends AggregatedInfoUtilityService {

  def aggregatedInfo(models: Model*): Future[Seq[AggregatedModelInfo]] = {
    val ids = models.map(_.id)
    for {
      builds <- modelBuildManagmentService.lastForModels(ids)
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

  override def allModelsAggregatedInfo(): Future[Seq[AggregatedModelInfo]] = {
    modelManagementService.allModels().flatMap(aggregatedInfo)
  }

  override def getModelAggregatedInfo(id: Long): HFResult[AggregatedModelInfo] = {
    modelManagementService.getModel(id).flatMap {
      case Left(err) => Result.errorF(err)
      case Right(model) =>
        aggregatedInfo(model).map(_.headOption).map{
          case Some(info) => Result.ok(info)
          case None => Result.clientError(s"Can't find aggregated info for model $id")
        }
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
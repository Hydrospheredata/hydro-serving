package io.hydrosphere.serving.manager.domain.build_script

import io.hydrosphere.serving.model.api.ModelType

import scala.concurrent.{ExecutionContext, Future}

trait BuildScriptServiceAlg {
  def fetchScriptForModelType(modelType: ModelType): Future[String]
}

class BuildScriptService(
  modelBuildScriptRepository: BuildScriptRepositoryAlgebra[Future]
)(implicit executionContext: ExecutionContext) extends BuildScriptServiceAlg {

  def fetchScriptForModelType(modelType: ModelType): Future[String] =
    modelBuildScriptRepository.get(modelType.toTag).flatMap {
      case Some(script) => Future.successful(script.script)
      case None => Future.successful(DefaultScript.genericBuildScript.script)
    }
}
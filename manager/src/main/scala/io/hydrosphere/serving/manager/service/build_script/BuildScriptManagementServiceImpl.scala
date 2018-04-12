package io.hydrosphere.serving.manager.service.build_script

import io.hydrosphere.serving.manager.model.db.Model
import io.hydrosphere.serving.manager.repository.ModelBuildScriptRepository

import scala.concurrent.{ExecutionContext, Future}

class BuildScriptManagementServiceImpl(
  modelBuildScriptRepository: ModelBuildScriptRepository
)(implicit executionContext: ExecutionContext) extends BuildScriptManagementService {

  def fetchScriptForModel(model: Model): Future[String] =
    modelBuildScriptRepository.get(model.modelType.toTag).flatMap {
      case Some(script) => Future.successful(script.script)
      case None => Future.successful(BuildScriptManagementServiceImpl.defaultBuildScript)
    }
}

object BuildScriptManagementServiceImpl {
  val defaultBuildScript =  """FROM busybox:1.28.0
           LABEL MODEL_TYPE={MODEL_TYPE}
           LABEL MODEL_NAME={MODEL_NAME}
           LABEL MODEL_VERSION={MODEL_VERSION}
           VOLUME /model
           ADD {MODEL_PATH} /model"""
}
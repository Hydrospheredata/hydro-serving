package io.hydrosphere.serving.manager.service.modelpush

import io.hydrosphere.serving.manager.model.{ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.service.modelbuild.docker.ProgressHandler

trait ModelPushService {
  def getImageName(modelBuild: ModelBuild): String = {
    modelBuild.model.name
  }

  def push(modelRuntime: ModelVersion, progressHandler: ProgressHandler)
}

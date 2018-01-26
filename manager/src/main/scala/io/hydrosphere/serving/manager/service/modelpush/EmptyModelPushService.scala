package io.hydrosphere.serving.manager.service.modelpush

import io.hydrosphere.serving.manager.model.ModelVersion
import io.hydrosphere.serving.manager.service.modelbuild.docker.ProgressHandler

class EmptyModelPushService extends ModelPushService {
  override def push(modelRuntime: ModelVersion, progressHandler: ProgressHandler): Unit = {}
}
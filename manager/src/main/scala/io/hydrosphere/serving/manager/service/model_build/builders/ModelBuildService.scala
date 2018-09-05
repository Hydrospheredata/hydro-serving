package io.hydrosphere.serving.manager.service.model_build.builders

import com.spotify.docker.client.ProgressHandler
import io.hydrosphere.serving.manager.model.db.{ModelBuild, ModelVersion}
import io.hydrosphere.serving.model.api.HFResult

trait ModelBuildService {
  val SCRIPT_VAL_MODEL_PATH = "MODEL_PATH"
  val SCRIPT_VAL_MODEL_TYPE = "MODEL_TYPE"
  val SCRIPT_VAL_MODEL_NAME = "MODEL_NAME"
  val SCRIPT_VAL_MODEL_VERSION = "MODEL_VERSION"

  def build(modelBuild: ModelBuild, imageName: String, script: String, progressHandler: ProgressHandler): HFResult[String]
}

trait ModelPushService {
  def getImageName(modelBuild: ModelBuild): String = {
    s"${modelBuild.model.name}:${modelBuild.version.toString}"
  }

  def push(modelRuntime: ModelVersion, progressHandler: ProgressHandler)
}

class EmptyModelPushService extends ModelPushService {
  override def push(modelRuntime: ModelVersion, progressHandler: ProgressHandler): Unit = {}
}


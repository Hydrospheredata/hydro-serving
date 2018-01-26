package io.hydrosphere.serving.manager.service.modelbuild

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service.modelbuild.docker.ProgressHandler

import scala.concurrent.Future

trait ModelBuildService {
  val SCRIPT_VAL_MODEL_PATH = "MODEL_PATH"
  val SCRIPT_VAL_MODEL_TYPE = "MODEL_TYPE"
  val SCRIPT_VAL_MODEL_NAME = "MODEL_NAME"
  val SCRIPT_VAL_MODEL_VERSION = "MODEL_VERSION"

  def build(modelBuild: ModelBuild, imageName: String, script: String, progressHandler: ProgressHandler): Future[String]
}

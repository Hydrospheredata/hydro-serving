package io.hydrosphere.serving.manager.domain.model_version

import com.spotify.docker.client.ProgressHandler
import io.hydrosphere.serving.manager.domain.image.DockerImage

trait ModelVersionPushAlgebra {
  def getImage(modelName: String, modelVersion: Long): DockerImage

  def push(modelVersion: ModelVersion, progressHandler: ProgressHandler)
}

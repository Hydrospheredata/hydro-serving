package io.hydrosphere.serving.manager.infrastructure.model.push

import com.spotify.docker.client.ProgressHandler
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionPushAlgebra}

class EmptyModelPushService extends ModelVersionPushAlgebra {

  override def getImage(modelName: String, modelVersion: Long): DockerImage = {
    DockerImage(
      name = modelName,
      tag = modelVersion.toString
    )
  }

  override def push(modelRuntime: ModelVersion, progressHandler: ProgressHandler): Unit = {}
}

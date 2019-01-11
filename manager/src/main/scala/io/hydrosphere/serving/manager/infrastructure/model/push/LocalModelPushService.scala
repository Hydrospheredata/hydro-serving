package io.hydrosphere.serving.manager.infrastructure.model.push

import com.spotify.docker.client.ProgressHandler
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionPushAlgebra}

/**
  * Push service used for local deployment.
  * Doesn't push any image, and uses standard modelname:modelversion image schema.
  */
class LocalModelPushService extends ModelVersionPushAlgebra {

  override def getImage(modelName: String, modelVersion: Long): DockerImage = {
    DockerImage(
      name = modelName,
      tag = modelVersion.toString
    )
  }

  override def push(modelRuntime: ModelVersion, progressHandler: ProgressHandler): Unit = {}
}

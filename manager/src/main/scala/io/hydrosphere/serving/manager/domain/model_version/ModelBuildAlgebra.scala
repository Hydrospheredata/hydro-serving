package io.hydrosphere.serving.manager.domain.model_version

import com.spotify.docker.client.ProgressHandler
import io.hydrosphere.serving.model.api.HFResult

trait ModelBuildAlgebra {
  def build(
    buildRequest: BuildRequest,
    progressHandler: ProgressHandler
  ): HFResult[String]
}

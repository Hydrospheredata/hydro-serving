package io.hydrosphere.serving.manager.domain.build_script

object DefaultScript {
  val genericBuildScript = ModelBuildScript(
    "generic_build_script",
    """FROM busybox:1.28.0
           LABEL MODEL_TYPE={MODEL_TYPE}
           LABEL MODEL_NAME={MODEL_NAME}
           LABEL MODEL_VERSION={MODEL_VERSION}
           VOLUME /model
           ADD {MODEL_PATH} /model"""
  )
}
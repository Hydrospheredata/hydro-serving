package io.hydrosphere.serving.manager.domain.model_build

import io.hydrosphere.serving.manager.domain.model_version.BuildRequest

object BuildScript {
  final def generate(buildRequest: BuildRequest): String = {
    val commandSeq = Seq(
      s"FROM ${buildRequest.baseImage.fullName}",
      s"LABEL ${Parameters.modelNameLabel}=${buildRequest.modelName}",
      s"LABEL ${Parameters.modelVersionLabel}=${buildRequest.modelVersion}",
      s"ADD model /model",
      s"WORKDIR /model"
    ) ++ buildRequest.installCommand
      .map(x => s"RUN $x").toSeq
    commandSeq.mkString("\n")
  }

  object Parameters {
    final val modelVersionLabel = "MODEL_VERSION"
    final val modelNameLabel = "MODEL_NAME"

    final val filesDir = "model/files"
    final val contractFile = "model/contract.protobin"
  }

}
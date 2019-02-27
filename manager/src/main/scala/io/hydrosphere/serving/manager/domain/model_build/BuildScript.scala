package io.hydrosphere.serving.manager.domain.model_build

import io.hydrosphere.serving.manager.domain.model_version.ModelVersion

object BuildScript {
  final def generate(modelVersion: ModelVersion): String = {
    val commandSeq = Seq(
      s"FROM ${modelVersion.runtime.fullName}",
      s"LABEL ${Parameters.modelNameLabel}=${modelVersion.model.name}",
      s"LABEL ${Parameters.modelVersionLabel}=${modelVersion.modelVersion}",
      s"ADD model /model",
      s"WORKDIR /model/files"
    ) ++ modelVersion.installCommand
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
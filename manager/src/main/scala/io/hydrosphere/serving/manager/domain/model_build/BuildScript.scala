package io.hydrosphere.serving.manager.domain.model_build

import java.nio.file.Path

import io.hydrosphere.serving.manager.domain.model_version.BuildRequest

object BuildScript {
  final def generate(buildRequest: BuildRequest, modelPath: Path): String = {
    s"""
       |FROM ${buildRequest.baseImage.fullName}
       |LABEL ${Parameters.modelNameLabel}=${buildRequest.modelName}
       |LABEL ${Parameters.modelVersionLabel}=${buildRequest.modelVersion}
       |ADD ${modelPath.toString} ${Parameters.modelFilesPath}
  """.stripMargin
  }

  object Parameters {
    final val modelVersionLabel = "MODEL_VERSION"
    final val modelNameLabel = "MODEL_NAME"
    final val modelFilesPath = "/model"

    final val modelRootDir = "model"
    final val modelFilesDir = s"$modelRootDir/files/"
    final val contractFile = s"$modelRootDir/contract.protobin"
  }

}
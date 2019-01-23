package io.hydrosphere.serving.manager.infrastructure.model_build

import java.io.{ByteArrayInputStream, IOException}
import java.nio.file.{Files, Path}

import io.hydrosphere.serving.manager.domain.model_build.{BuildScript, ModelFilePacker}
import io.hydrosphere.serving.manager.domain.model_version.BuildRequest
import io.hydrosphere.serving.manager.infrastructure.storage.{ModelStorage, StorageOps}
import org.apache.commons.io.FileUtils

import scala.concurrent.{ExecutionContext, Future}

class TempFilePacker(modelStorage: ModelStorage)(implicit ec: ExecutionContext) extends ModelFilePacker[Future] {
  override def pack(buildRequest: BuildRequest): Future[Path] = Future {
    val originalModelDir = modelStorage.getReadableFile(buildRequest.modelName).right.getOrElse(throw new IOException(s"Couldn't find directory for ${buildRequest.modelName} model"))
    val buildPath = Files.createTempDirectory(s"hs-model-${buildRequest.modelName}-${buildRequest.modelVersion}")

    // create Dockerfile
    Files.copy(new ByteArrayInputStream(BuildScript.generate(buildRequest, buildPath).getBytes), buildPath.resolve("Dockerfile"))

    // prepare and copy model files
    Files.createDirectories(buildPath.resolve(BuildScript.Parameters.modelRootDir))
    FileUtils.copyDirectory(originalModelDir, buildPath.resolve(BuildScript.Parameters.modelFilesPath).toFile)

    // write contract to the file
    Files.write(buildPath.resolve(BuildScript.Parameters.contractFile), buildRequest.contract.toByteArray)

    buildPath
  }
}

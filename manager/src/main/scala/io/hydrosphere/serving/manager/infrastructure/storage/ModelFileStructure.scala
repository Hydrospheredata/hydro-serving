package io.hydrosphere.serving.manager.infrastructure.storage

import java.nio.file.Path

case class ModelFileStructure(
  root: Path,
  model: Path,
  filesPath: Path,
  contractPath: Path,
  dockerfile: Path
)

object ModelFileStructure {
  def forRoot(root: Path): ModelFileStructure = {
    val modelPath = root.resolve("model")
    ModelFileStructure(
      root = root,
      model = modelPath,
      filesPath = modelPath.resolve("files"),
      contractPath = modelPath.resolve("contract.protobin"),
      dockerfile = root.resolve("Dockerfile")
    )
  }
}
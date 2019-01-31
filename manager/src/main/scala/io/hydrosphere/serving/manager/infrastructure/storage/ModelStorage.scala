package io.hydrosphere.serving.manager.infrastructure.storage

import java.nio.file.Path

import cats.effect.Sync

case class ModelFileStructure(
  root: Path,
  model: Path,
  filesPath: Path,
  contractPath: Path,
  dockerfile: Path
)

object ModelFileStructure {
  def forRoot[F[_]: Sync](root: Path): F[ModelFileStructure] = Sync[F].delay {
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

trait ModelStorage[F[_]] {
  /**
    * Unpacks model files and returns path to it
    * @param filePath path to the tarball file
    * @return
    */
  def unpack(filePath: Path): F[ModelFileStructure]
}

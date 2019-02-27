package io.hydrosphere.serving.manager.infrastructure.storage.fetchers

import java.nio.file.Path

import cats.Monad
import cats.data.OptionT
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.infrastructure.storage.StorageOps
import org.apache.logging.log4j.scala.Logging

import scala.util.Try

class FallbackContractFetcher[F[_]: Monad](
  source: StorageOps[F]
) extends ModelFetcher[F] with Logging {
  override def fetch(directory: Path): F[Option[FetcherResult]] = {
    OptionT(getContract(directory)).map { contract =>
      FetcherResult(
        modelName = directory.getFileName.toString,
        modelContract = contract,
        metadata = Map.empty
      )
    }.value
  }

  private def getContract(modelPath: Path): F[Option[ModelContract]] = {
    val txtContract = for {
      metaFile <- OptionT(source.readText(modelPath.resolve("contract.prototxt")))
      text = metaFile.mkString
      contract <- OptionT.fromOption(Try(ModelContract.fromAscii(text)).toOption)
    } yield contract

    val binContract = for {
      metaFile <- OptionT(source.readBytes(modelPath.resolve("contract.protobin")))
      contract <- OptionT.fromOption(Try(ModelContract.parseFrom(metaFile)).toOption)
    } yield contract

    txtContract.orElse(binContract).value
  }
}
package io.hydrosphere.serving.manager.infrastructure.storage.fetchers

import java.nio.file.Path

import cats.Traverse
import cats.instances.future._
import cats.instances.list._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.infrastructure.storage.StorageOps
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.keras.KerasFetcher
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark.SparkModelFetcher
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.tensorflow.TensorflowModelFetcher
import io.hydrosphere.serving.model.api.{ModelMetadata, ModelType}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future


trait ModelFetcher[F[_]] {
  def fetch(source: StorageOps[Future], path: Path): F[Option[ModelMetadata]]
}

object ModelFetcher extends Logging {
  private[this] val fetchers = List(
    SparkModelFetcher,
    TensorflowModelFetcher,
    KerasFetcher,
    ONNXFetcher,
    FallbackContractFetcher[Future]
  )

  def fetch(storageOps: StorageOps[Future], path: Path): ModelMetadata = {
    val res = fetchers
      .map(_.fetch(storageOps, path))

    Traverse[List].traverse(fetchers)(_.fetch(storageOps, path))

    val model = res.flatten
      .headOption
      .getOrElse {
        ModelMetadata(path.getFileName.toString, ModelType.Unknown("unknown", "unknown"), ModelContract())
      }
    model
  }
}
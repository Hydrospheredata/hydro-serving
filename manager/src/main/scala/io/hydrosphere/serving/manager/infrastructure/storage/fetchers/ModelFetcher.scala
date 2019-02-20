package io.hydrosphere.serving.manager.infrastructure.storage.fetchers

import java.nio.file.Path

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.functor._
import cats.{Monad, Traverse}
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.infrastructure.storage.StorageOps
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.keras.KerasFetcher
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark.SparkModelFetcher
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.tensorflow.TensorflowModelFetcher
import org.apache.logging.log4j.scala.Logging

case class FetcherResult(
  modelName: String,
  modelContract: ModelContract,
  metadata: Map[String, String]
)

trait ModelFetcher[F[_]] {
  def fetch(path: Path): F[Option[FetcherResult]]
}

object ModelFetcher extends Logging {
  def default[F[_] : Sync](storageOps: StorageOps[F]) = {
    combine(Seq(
      new SparkModelFetcher[F](storageOps),
      new TensorflowModelFetcher[F](storageOps),
      new ONNXFetcher[F](storageOps),
      new KerasFetcher[F](storageOps),
      new FallbackContractFetcher[F](storageOps)
    ))
  }

  /**
    * Sequentially applies fetchers and returns the first successful result
    *
    * @param fetchers
    * @tparam F
    * @return
    */
  def combine[F[_] : Monad](fetchers: Seq[ModelFetcher[F]]) =
    new ModelFetcher[F] {
      override def fetch(path: Path): F[Option[FetcherResult]] = {
        val res = Traverse[List].traverse(fetchers.toList) { fetcher =>
          fetcher.fetch(path)
        }
        for {
          fetchResults <- res
        } yield {
          fetchResults.flatten.headOption
        }
      }
    }
}
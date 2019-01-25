package io.hydrosphere.serving.manager.infrastructure.storage.fetchers.keras

import java.nio.file.Path

import cats.data.OptionT
import cats.effect.Sync
import io.hydrosphere.serving.manager.infrastructure.storage.StorageOps
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.{FetcherResult, ModelFetcher}

class KerasFetcher[F[_]: Sync](source: StorageOps[F]) extends ModelFetcher[F] {
  override def fetch(directory: Path): F[Option[FetcherResult]] = {
    val f = for {
      importer <- OptionT(ModelConfigParser.importer(source, directory))
      model <- OptionT(importer.importModel)
    } yield model
    f.value
  }
}





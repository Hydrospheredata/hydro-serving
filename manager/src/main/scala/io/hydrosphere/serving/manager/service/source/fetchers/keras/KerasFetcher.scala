package io.hydrosphere.serving.manager.service.source.fetchers.keras

import io.hydrosphere.serving.manager.service.source.fetchers.ModelFetcher
import io.hydrosphere.serving.manager.service.source.storages.ModelStorage
import io.hydrosphere.serving.model.api.ModelMetadata
import org.apache.logging.log4j.scala.Logging

import scala.util.control.NonFatal

object KerasFetcher extends ModelFetcher with Logging {
  override def fetch(source: ModelStorage, directory: String): Option[ModelMetadata] = {
    try {
      for {
        importer <- ModelConfigParser.importer(source, directory)
        model <- importer.importModel
      } yield model
    } catch {
      case NonFatal(x) =>
        logger.warn(x)
        None
    }
  }
}





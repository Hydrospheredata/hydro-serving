package io.hydrosphere.serving.manager.service.source

import java.nio.file.Path

import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.controller.model_source.{AddLocalSourceRequest, AddS3SourceRequest}
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelMetadata
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.service.source.storages.ModelStorage

import scala.concurrent.Future

trait ModelStorageManagementService {
  /**
    * Perform an upload operation and return inferred metadata
    * @param upload
    * @return
    */
  def upload(upload: ModelUpload): HFResult[StorageUploadResult]
  /***
    * Try to fetch a model metadata from specified source path
    * @param source string which describes a path inside a source
    * @return If path doesn't exist - None. Some metadata otherwise
    */
  def index(source: String): HFResult[Option[ModelMetadata]]

  /***
    * List all the sources (db ++ config)
    * @return List of all sources
    */
  def getSources: Future[List[ModelStorage]]

  /***
    * Get readable path from url
    * @param sourcePath in-source url that defines some resource
    * @return readable path object
    */
  def getLocalPath(sourcePath: StoragePath): HFResult[Path]

  /***
    * Try to get source by name
    * @param name name of a source
    * @return source
    */
  def getSource(name: String): HFResult[ModelStorage]
}

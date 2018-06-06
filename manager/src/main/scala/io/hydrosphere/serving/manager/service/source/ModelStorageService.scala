package io.hydrosphere.serving.manager.service.source

import java.nio.file.Path

import io.hydrosphere.serving.manager.model._

trait ModelStorageService {
  /**
    * Perform an upload operation and return inferred metadata
    * @return
    */
  def upload(modelTarball: Path, maybeName: Option[String]): HFResult[StorageUploadResult]

  /**
    * Get readable path from folder in default localStorage
    * @param folderPath inner localStorage path
    * @return
    */
  def getLocalPath(folderPath: String): HFResult[Path]
}

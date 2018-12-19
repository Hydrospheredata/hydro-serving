package io.hydrosphere.serving.manager.infrastructure.storage

import java.nio.file.Path

import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.model.api.HFResult

trait ModelStorageService {
  /**
    * Perform an upload operation and return inferred metadata
    * @param upload
    * @return
    */
  def upload(filePath: Path, meta: ModelUploadMetadata): HFResult[StorageUploadResult]

  /**
    * Get readable path from folder in default localStorage
    * @param folderPath inner localStorage path
    * @return
    */
  def getLocalPath(folderPath: String): HFResult[Path]

  /***
    * Renames storage-level folder
    * @param oldFolder old folder name
    * @param newFolder new folder name
    * @return path to the new folder
    */
  def rename(oldFolder: String, newFolder: String): HFResult[Path]
}

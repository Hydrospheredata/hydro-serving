package io.hydrosphere.serving.manager.service.source

import java.nio.file.Path

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelMetadata

trait ModelStorageService {
  /**
    * Perform an upload operation and return inferred metadata
    * @param modelTarball
    * @param maybeName
    * @return
    */
  def upload(modelTarball: Path, maybeName: Option[String]): HFResult[ModelMetadata]

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

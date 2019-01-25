package io.hydrosphere.serving.manager.infrastructure.storage

import java.io.File
import java.nio.file.Path

trait ModelStorage[F[_]] {
  /**
    * Perform an upload operation and return path to model folder
    * @param upload
    * @return
    */
  def unpack(filePath: Path, folderName: Option[String]): F[Path]

  /**
    * Get readable path from model in default localStorage
    * @param folderPath inner localStorage path
    * @return
    */
  def getLocalPath(folderPath: String): F[Option[Path]]

  /***
    * Renames storage-level folder
    * @param oldFolder old folder name
    * @param newFolder new folder name
    * @return path to the new folder
    */
  def rename(oldFolder: String, newFolder: String): F[Option[Path]]

  def writeFile(path: String, localFile: File): F[Path]
}

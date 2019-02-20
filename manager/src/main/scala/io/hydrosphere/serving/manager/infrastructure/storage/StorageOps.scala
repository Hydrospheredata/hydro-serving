package io.hydrosphere.serving.manager.infrastructure.storage

import java.io.File
import java.nio.file.Path

import cats.effect.Sync

trait StorageOps[F[_]] {
  def getReadableFile(path: Path): F[Option[File]]

  def getAllFiles(folder: Path): F[Option[List[String]]]

  def getSubDirs(path: Path): F[Option[List[String]]]

  def exists(path: Path): F[Boolean]

  def copyFile(src: Path, target: Path): F[Path]

  def moveFolder(src: Path, target: Path): F[Path]

  def removeFolder(path: Path): F[Option[Unit]]

  def getTempDir(prefix: String): F[Path]
  
  def readText(path: Path): F[Option[List[String]]]

  def readBytes(path: Path): F[Option[Array[Byte]]]

  def writeBytes(path: Path, bytes: Array[Byte]): F[Path]
}

object StorageOps {
  def default[F[_]: Sync] = new LocalStorageOps[F]()
}
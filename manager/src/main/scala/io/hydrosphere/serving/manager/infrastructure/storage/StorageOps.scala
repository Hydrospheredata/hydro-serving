package io.hydrosphere.serving.manager.infrastructure.storage

import java.io.File
import java.nio.file.Path

import cats.effect.Sync

trait StorageOps[F[_]] {
  def getReadableFile(path: Path): F[File]

  def getAllFiles(folder: Path): F[Seq[String]]

  def getSubDirs(path: Path): F[Seq[String]]

  def exists(path: Path): F[Boolean]

  def copyFile(src: Path, target: Path): F[Path]

  def removeFolder(path: Path): F[Unit]
}

object StorageOps {
  def default[F[_]: Sync] = new LocalStorageOps[F]()
}
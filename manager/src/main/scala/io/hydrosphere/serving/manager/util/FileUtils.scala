package io.hydrosphere.serving.manager.util

import java.io._
import java.nio.file.{FileVisitResult, FileVisitor, Files, Path}
import java.nio.file.attribute.BasicFileAttributes

import org.apache.logging.log4j.scala.Logging


object FileUtils {

  class RecursiveRemover extends FileVisitor[Path] with Logging {
    def visitFileFailed(file: Path, exc: IOException) = {
      logger.warn(s"Error while visiting file: ${exc.getMessage}")
      FileVisitResult.CONTINUE
    }

    def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      Files.delete(file)
      FileVisitResult.CONTINUE
    }

    def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE

    def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
      Option(exc).foreach { ex =>
        logger.warn(s"Error in post visiting directory: ${ex.getMessage}")
      }
      Files.delete(dir)
      FileVisitResult.CONTINUE
    }
  }

  implicit class PumpedFile(file: File) {
    def getSubDirectories: Seq[File] = {
      Option(file.listFiles())
        .getOrElse(Array.empty)
        .filter(_.isDirectory)
    }

    def listFilesRecursively: Seq[File] = {
      def _listChildFiles(f: File): Seq[File] = {
        if (f.isFile)
          Seq(f)
        else {
          f.listFiles().flatMap(_listChildFiles).toList
        }
      }

      if (file.isDirectory) {
        _listChildFiles(file)
      } else {
        Seq.empty
      }
    }
  }


  def getResourcePath(resPath: String): String = {
    Option(getClass.getClassLoader.getResource(resPath))
      .map(_.getPath)
      .getOrElse(throw new FileNotFoundException(s"$resPath not found in resources"))
  }
}
package io.hydrosphere.serving.manager.util

import java.io._

/**
  *
  */
object FileUtils {

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

}
package io.hydrosphere.serving.manager.util

import java.io._

/**
  *
  */
object FileUtils {

  implicit class PumpedFile(file: File) {
    def getSubDirectories: List[File] = {
      if (file.listFiles() != null) {
        file.listFiles().filter(_.isDirectory).toList
      } else {
        List.empty
      }
    }

    def listFilesRecursively: List[File] = {
      if (file.isDirectory) {
        _listChildFiles(file)
      } else {
        List.empty
      }
    }

    private def _listChildFiles(f: File): List[File] = {
      if (f.isFile)
        List(f)
      else {
        f.listFiles().flatMap(_listChildFiles).toList
      }
    }
  }

}
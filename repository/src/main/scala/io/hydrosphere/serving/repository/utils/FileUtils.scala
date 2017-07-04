package io.hydrosphere.serving.repository.utils

import java.io._

/**
  * Created by Bulat on 31.05.2017.
  */
object FileUtils {
  implicit class PumpedFile(file: File) {
    def getSubDirectories: Array[File] = file.listFiles().filter(_.isDirectory)

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

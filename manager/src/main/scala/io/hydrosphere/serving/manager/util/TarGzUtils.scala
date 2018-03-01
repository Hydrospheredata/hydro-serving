package io.hydrosphere.serving.manager.util

import java.io.{BufferedOutputStream, File, FileOutputStream}
import java.nio.file.{Files, Path}

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils

import scala.collection.mutable

object TarGzUtils {
  def decompress(tarballPath: Path, output: Path): Seq[Path] = {
    val unpackedFiles = mutable.ListBuffer.empty[Path]
    val out = output.toFile
    val fin = Option(new TarArchiveInputStream(new GzipCompressorInputStream(Files.newInputStream(tarballPath))))
    fin.foreach { stream =>
      var entry = stream.getNextTarEntry
      while (entry != null) {
        val file = new File(out, entry.getName)
        if (!file.isHidden) {
          if (entry.isDirectory) {
            if (!file.exists()) {
              file.mkdirs()
            }
          } else {
            val parent = file.getParentFile
            if (!parent.exists()) {
              parent.mkdirs()
            }
            val outstream = new BufferedOutputStream(new FileOutputStream(file))
            val res = IOUtils.copy(stream, outstream)
            outstream.flush()
            outstream.close()
            unpackedFiles += file.toPath
          }
        }
        entry = stream.getNextTarEntry
      }

      stream.close()
    }
    unpackedFiles.toList
  }
}

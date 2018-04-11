package io.hydrosphere.serving.manager.util

import java.io._
import java.nio.file.{Files, Path}

import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream, TarArchiveOutputStream}
import org.apache.commons.compress.compressors.gzip.{GzipCompressorInputStream, GzipCompressorOutputStream}
import org.apache.commons.compress.utils.IOUtils

import scala.collection.mutable

object TarGzUtils {
  def compress(input: Path, tarballPath: Path, dir: Option[String] = None): Unit = {
    val fout = Option(new TarArchiveOutputStream(new GzipCompressorOutputStream(new FileOutputStream(tarballPath.toFile))))
    fout.foreach { stream =>
      stream.setAddPaxHeadersForNonAsciiNames(true)
      stream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
      compressItem(stream, input.toFile, dir)
      stream.close()
    }
  }

  def compressItem(tarStream: TarArchiveOutputStream, file: File, dir: Option[String]): Unit = {
    val entry = dir.map(_ + File.separator).getOrElse("") + file.getName
    if (file.isDirectory) {
      println(s"TAR dir: $entry")
      file.listFiles().foreach { subFile =>
        compressItem(tarStream, subFile, Some(entry))
      }
    } else {
      println(s"TAR file: $entry")
      tarStream.putArchiveEntry(new TarArchiveEntry(file, entry))
      val in = new FileInputStream(file)
      val bin = new BufferedInputStream(in)
      IOUtils.copy(bin, tarStream)
      bin.close()
      tarStream.closeArchiveEntry()
    }
  }

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

package io.hydrosphere.serving.manager.service.source

import io.hydrosphere.serving.manager.model.{HResult, Result}
import io.hydrosphere.serving.manager.model.Result.ClientError
import Result.Implicits._

case class SourcePath(sourceName: String, path: String)

object SourcePath {
  def parse(source: String): HResult[SourcePath] = {
    val args = source.split(':')
    if (args.length == 2) {
      for {
        sourceName <- args.headOption.toHResult(ClientError("Source name is not defined")).right
        path <- args.lastOption.toHResult(ClientError("Source path is not defined")).right
      } yield SourcePath(sourceName, path)
    } else {
      Result.clientError("Incorrect source path")
    }
  }
}
package io.hydrosphere.serving.manager.infrastructure.storage.fetchers.keras

import java.nio.file.Path

import cats.Monad
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.infrastructure.storage.StorageOps
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.FetcherResult
import io.hydrosphere.serving.manager.util.HDF5File
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.scala.Logging

import scala.util.Try

private[keras] trait ModelConfigParser[F[_]] {
  def importModel: F[Option[FetcherResult]]
}

private[keras] object ModelConfigParser extends Logging {
  def importer[F[_] : Sync](source: StorageOps[F], directory: Path): F[Option[ModelConfigParser[F]]] = {
    val f = for {
      h5Path <- findH5file(source, directory)
    } yield ModelConfigParser.H5(source, h5Path).asInstanceOf[ModelConfigParser[F]]
    f.value
  }

  def findH5file[F[_] : Monad](source: StorageOps[F], directory: Path) = {
    for {
      dirFile <- OptionT(source.getReadableFile(directory))
      file <- OptionT.fromOption(dirFile.listFiles().find(f => f.isFile && f.getName.endsWith(".h5")).map(_.toPath))
    } yield file
  }

  case class H5[F[_] : Sync](source: StorageOps[F], h5path: Path) extends ModelConfigParser[F] {
    def importModel: F[Option[FetcherResult]] = {
      val h5 = Sync[F].delay(HDF5File(h5path.toString))
      Sync[F].bracketCase(h5) { h5File =>
        for {
          modelName <- Sync[F].delay(FilenameUtils.removeExtension(h5path.getFileName.getFileName.toString))
          jsonModelConfig <- Sync[F].delay(h5File.readAttributeAsString("model_config"))
          kerasVersion <- Sync[F].delay(h5File.readAttributeAsString("keras_version"))
          model <- JsonString(jsonModelConfig, modelName, kerasVersion).importModel
        } yield model
      } {
        case (a, _) => Sync[F].delay(a.close())
      }
    }
  }

  //  case class JsonFile(jsonPath: Path) extends ModelConfigParser {
  //    def importModel: Option[ModelMetadata] = {
  //      val jsonModelConfig = Source.fromFile(jsonPath.toFile).mkString
  //      JsonString(jsonModelConfig, jsonPath.toString, "unknown").importModel
  //    }
  //  }

  case class JsonString[F[_] : Monad](modelConfigJson: String, name: String, version: String) extends ModelConfigParser[F] {

    import spray.json._

    override def importModel: F[Option[FetcherResult]] = {
      val f = for {
        config <- OptionT.fromOption(Try(modelConfigJson.parseJson.convertTo[ModelConfig]).toOption)
        signatures <- OptionT.fromOption(Try(config.toSignatures).toOption)
        contract = ModelContract(
          modelName = name,
          signatures = signatures
        )
      } yield {
        FetcherResult(
          modelName = name,
          modelContract = contract,
          metadata = Map.empty
        )
      }
      f.value
    }
  }

}
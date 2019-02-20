package io.hydrosphere.serving.manager.infrastructure.storage.fetchers
import java.nio.file.{Files, Path}

import cats.Monad
import cats.data.OptionT
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.infrastructure.storage.StorageOps
import io.hydrosphere.serving.onnx.onnx.TensorProto.DataType._
import io.hydrosphere.serving.onnx.onnx._
import io.hydrosphere.serving.tensorflow.tensor_shape
import io.hydrosphere.serving.tensorflow.types.DataType
import org.apache.commons.io.FilenameUtils

import scala.util.Try

class ONNXFetcher[F[_]: Monad](
  storageOps: StorageOps[F]
) extends ModelFetcher[F] {
  def findFile(directory: Path): OptionT[F, Path] = {
    for {
      dirFile <- OptionT(storageOps.getReadableFile(directory))
      onnxFile <- OptionT.fromOption(dirFile.listFiles().find(f => f.isFile && f.getName.endsWith(".onnx")).map(_.toPath))
    } yield onnxFile
  }

  def modelMetadata(model: ModelProto): Map[String, String] = {
    val basic = Map(
      "producerName" -> model.producerName,
      "producerVersion" -> model.producerVersion,
      "domain" -> model.domain,
      "irVersion" -> model.irVersion.toString,
      "modelVersion" -> model.modelVersion.toString,
      "docString" -> model.docString,
    ).mapValues(_.trim)
      .filter { // filter proto default strings
        case (_, s) => s.nonEmpty
      }

    val props = model.metadataProps.map { x =>
      x.key -> x.value
    }.toMap

    basic ++ props
  }

  override def fetch(directory: Path): F[Option[FetcherResult]] = {
    val f = for {
      filePath <- findFile(directory)
      fileName = FilenameUtils.getBaseName(filePath.getFileName.toString)
      model <- OptionT.fromOption(ModelProto.validate(Files.readAllBytes(filePath)).toOption)
      graph <- OptionT.fromOption(model.graph)
      signatures <- OptionT.fromOption(Try(ONNXFetcher.extractSignatures(graph)).toOption)
    } yield {
      FetcherResult(
        fileName,
        ModelContract(fileName, signatures),
        metadata = modelMetadata(model)
      )
    }
    f.value
  }
}

object ONNXFetcher {
  final val signature = "infer"

  def convertType(elemType: TensorProto.DataType): ModelField.TypeOrSubfields.Dtype = {
    val tfType = elemType match {
      case UNDEFINED => DataType.DT_INVALID
      case FLOAT => DataType.DT_FLOAT
      case UINT8 => DataType.DT_UINT8
      case INT8 => DataType.DT_INT8
      case UINT16 => DataType.DT_UINT16
      case INT16 => DataType.DT_INT16
      case INT32 => DataType.DT_INT32
      case INT64 => DataType.DT_INT64
      case STRING => DataType.DT_STRING
      case BOOL => DataType.DT_BOOL
      case FLOAT16 => DataType.DT_FLOAT
      case DOUBLE => DataType.DT_DOUBLE
      case UINT32 => DataType.DT_UINT32
      case UINT64 => DataType.DT_UINT64
      case COMPLEX64 => DataType.DT_COMPLEX64
      case COMPLEX128 => DataType.DT_COMPLEX128
      case Unrecognized(value) => DataType.Unrecognized(value)
    }
    ModelField.TypeOrSubfields.Dtype(tfType)
  }

  def convertShape(shape: Option[TensorShapeProto]): Option[tensor_shape.TensorShapeProto] = {
    shape.map { realShape =>
      val dims = realShape.dim.map { realDim =>
        tensor_shape.TensorShapeProto.Dim(realDim.value.dimValue.get)
      }
      tensor_shape.TensorShapeProto(dims)
    }
  }

  def valueInfoToField(x: ValueInfoProto): ModelField = {
    ModelField(
      x.name,
      convertShape(x.getType.getTensorType.shape),
      convertType(x.getType.getTensorType.elemType)
    )
  }

  def extractSignatures(graph: GraphProto): Seq[ModelSignature] = {
    Seq(ModelSignature(
      signature,
      graph.input.map(valueInfoToField),
      graph.output.map(valueInfoToField)
    ))
  }
}
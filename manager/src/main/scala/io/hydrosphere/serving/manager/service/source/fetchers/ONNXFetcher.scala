package io.hydrosphere.serving.manager.service.source.fetchers
import java.nio.file.{Files, Path}

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.model.api.ModelMetadata
import io.hydrosphere.serving.model.api.ModelType.ONNX
import io.hydrosphere.serving.manager.service.source.storages.ModelStorage
import io.hydrosphere.serving.onnx.onnx.TensorProto.DataType._
import io.hydrosphere.serving.onnx.onnx._
import io.hydrosphere.serving.tensorflow.tensor_shape
import io.hydrosphere.serving.tensorflow.types.DataType
import org.apache.commons.io.FilenameUtils

object ONNXFetcher extends ModelFetcher {
  val signature = "infer"

  def convertShape(shape: Option[TensorShapeProto]): Option[tensor_shape.TensorShapeProto] = {
    shape.map { realShape =>
      val dims = realShape.dim.map { realDim =>
        tensor_shape.TensorShapeProto.Dim(realDim.value.dimValue.get)
      }
      tensor_shape.TensorShapeProto(dims)
    }
  }

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

  def findFile(source: ModelStorage, directory: String): Option[Path] = {
    source.getReadableFile(directory).right.toOption.flatMap { dirFile =>
      dirFile.listFiles().find(f => f.isFile && f.getName.endsWith(".onnx")).map(_.toPath)
    }
  }

  override def fetch(source: ModelStorage, directory: String): Option[ModelMetadata] = {
    for {
      filePath <- findFile(source, directory)
      fileName = FilenameUtils.getBaseName(filePath.getFileName.toString)
      model <- ModelProto.validate(Files.readAllBytes(filePath)).toOption
      graph <- model.graph
    } yield {
      ModelMetadata(
        fileName,
        ONNX(model.producerName, model.producerVersion),
        ModelContract(fileName, extractSignatures(graph))
      )
    }
  }
}

package io.hydrosphere.serving.manager.infrastructure.storage.fetchers.tensorflow

import java.nio.file.Path

import cats.Monad
import cats.data.OptionT
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.infrastructure.storage.StorageOps
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.{FetcherResult, FieldInfo, ModelFetcher}
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.tensorflow.framework.{SavedModel, SignatureDef, TensorInfo}

import scala.collection.JavaConverters._
import scala.util.Try

class TensorflowModelFetcher[F[_]: Monad](storageOps: StorageOps[F]) extends ModelFetcher[F] {

  override def fetch(directory: Path): F[Option[FetcherResult]] = {
    val f = for {
      savedModelBytes <- OptionT(storageOps.readBytes(directory.resolve("saved_model.pb")))
      savedModel <- OptionT.fromOption(Try(SavedModel.parseFrom(savedModelBytes)).toOption)
      signatures <- OptionT.fromOption(
        Try(savedModel.getMetaGraphsList.asScala.flatMap { metagraph =>
          metagraph.getSignatureDefMap.asScala.map(TensorflowModelFetcher.convertSignature).toList
        }.toList).toOption
      )
      modelName = directory.getFileName.toString
    } yield FetcherResult(
      modelName = modelName,
      modelContract = ModelContract(modelName, signatures)
    )
    f.value
  }
}

object TensorflowModelFetcher {
  def convertTensor(tensorInfo: TensorInfo): FieldInfo = {
    val shape = if (tensorInfo.hasTensorShape) {
      val tShape = tensorInfo.getTensorShape
      Some(TensorShapeProto(tShape.getDimList.asScala.map(x => TensorShapeProto.Dim(x.getSize, x.getName)), tShape.getUnknownRank))
    } else None
    val convertedDtype = DataType.fromValue(tensorInfo.getDtypeValue)
    FieldInfo(convertedDtype, TensorShape(shape))
  }

  def convertTensorMap(tensorMap: Map[String, TensorInfo]): List[ModelField] = {
    tensorMap.map {
      case (inputName, inputDef) =>
        val convertedInputDef = convertTensor(inputDef)
        ModelField(
          name = inputName,
          shape = convertedInputDef.shape.toProto,
          typeOrSubfields = ModelField.TypeOrSubfields.Dtype(convertedInputDef.dataType)
        )
    }.toList
  }

  def convertSignature(signatureKV: (String, SignatureDef)): ModelSignature = {
    ModelSignature(
      signatureName = signatureKV._1,
      inputs = convertTensorMap(signatureKV._2.getInputsMap.asScala.toMap),
      outputs = convertTensorMap(signatureKV._2.getOutputsMap.asScala.toMap)
    )
  }
}
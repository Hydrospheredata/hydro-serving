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

  def parseMetadata(savedModel: SavedModel): Map[String, String] = {
    val metagraphdata = savedModel.getMetaGraphsList.asScala.zipWithIndex.flatMap {
      case (mg, id) =>
        val collectionsCount = Option(mg.getCollectionDefCount)
        val assetFilesCount = Option(mg.getAssetFileDefCount)
        val serializedSize = Option(mg.getSerializedSize)
        val signatureCount = Option(mg.getSignatureDefCount)
        val metaInfo = Option(mg.getMetaInfoDef)
        val mgVersion = metaInfo.flatMap(x => Option(x.getMetaGraphVersion))
        val strippedDefaultAttrs = metaInfo.flatMap(x => Option(x.getStrippedDefaultAttrs))
        val tagsCount = metaInfo.flatMap(x => Option(x.getTagsCount))
        val tensorflowGitVersion = metaInfo.flatMap(x => Option(x.getTensorflowGitVersion))
        val tensorflowVersion = metaInfo.flatMap(x => Option(x.getTensorflowVersion))
        Map(
          s"$id/collectionsCount" -> collectionsCount,
          s"$id/assetFilesCount" -> assetFilesCount,
          s"$id/serializedSize" -> serializedSize,
          s"$id/signatureCount" -> signatureCount,
          s"$id/metaGraphVersion" -> mgVersion,
          s"$id/strippedDefaultAttrs" -> strippedDefaultAttrs,
          s"$id/tagsCount" -> tagsCount,
          s"$id/tensorflowGitVersion" -> tensorflowGitVersion,
          s"$id/tensorflowVersion" -> tensorflowVersion,
        ).collect({ case (k, Some(v)) => k -> v.toString })
    }.toMap

    val mgCount = Option(savedModel.getMetaGraphsCount)
    val savedModelData = Map("metaGraphsCount" -> mgCount).collect({ case (k, Some(v)) => k -> v.toString })

    val all = (metagraphdata ++ savedModelData)
      .mapValues(_.trim)
      .filter { // filter proto default strings
        case (_, s) => s.nonEmpty
      }
    all
  }

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
      modelContract = ModelContract(modelName, signatures),
      metadata = parseMetadata(savedModel)
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
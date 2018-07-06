package io.hydrosphere.serving.manager.service.source.fetchers

import java.nio.file.Files

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.api.{ModelMetadata, _}
import io.hydrosphere.serving.manager.service.source.storages.ModelStorage
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.apache.logging.log4j.scala.Logging
import org.tensorflow.framework.{SavedModel, SignatureDef, TensorInfo}

import scala.collection.JavaConverters._

object TensorflowModelFetcher extends ModelFetcher with Logging {

  override def fetch(source: ModelStorage, directory: String): Option[ModelMetadata] = {
    source.getReadableFile(s"$directory/saved_model.pb") match {
      case Left(error) =>
        logger.debug(s"Fetch error: $error. $directory in not a valid Tensorflow model")
        None
      case Right(pbFile) =>
        val savedModel = SavedModel.parseFrom(Files.newInputStream(pbFile.toPath))
        val version = savedModel.getMetaGraphsList.asScala.headOption.map(_.getMetaInfoDef.getTensorflowVersion).getOrElse("unknown")
        val signatures = savedModel
          .getMetaGraphsList
          .asScala
          .flatMap { metagraph =>
            metagraph.getSignatureDefMap.asScala.map(convertSignature).toList
          }

        Some(
          ModelMetadata(
            modelName = directory,
            contract = ModelContract(
              directory,
              signatures
            ),
            modelType = ModelType.Tensorflow(version)
          )
        )
    }
  }

  private def convertTensor(tensorInfo: TensorInfo): FieldInfo = {
    val shape = if (tensorInfo.hasTensorShape) {
      val tShape = tensorInfo.getTensorShape
      Some(TensorShapeProto(tShape.getDimList.asScala.map(x => TensorShapeProto.Dim(x.getSize, x.getName)), tShape.getUnknownRank))
    } else None
    val convertedDtype = DataType.fromValue(tensorInfo.getDtypeValue)
    FieldInfo(convertedDtype, TensorShape(shape))
  }

  private def convertTensorMap(tensorMap: Map[String, TensorInfo]): List[ModelField] = {
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

  private def convertSignature(signatureKV: (String, SignatureDef)): ModelSignature = {
    ModelSignature(
      signatureName = signatureKV._1,
      inputs = convertTensorMap(signatureKV._2.getInputsMap.asScala.toMap),
      outputs = convertTensorMap(signatureKV._2.getOutputsMap.asScala.toMap)
    )
  }
}

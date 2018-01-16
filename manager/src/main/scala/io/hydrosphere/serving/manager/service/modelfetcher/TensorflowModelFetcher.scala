package io.hydrosphere.serving.manager.service.modelfetcher

import java.io.FileNotFoundException
import java.nio.file.{Files, NoSuchFileException}

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import io.hydrosphere.serving.model_api._
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging
import org.tensorflow.framework.{SavedModel, SignatureDef, TensorInfo => TFTensorInfo}

import scala.collection.JavaConversions._
/**
  * Created by bulat on 24/07/2017.
  */
object TensorflowModelFetcher extends ModelFetcher with Logging {

  override def fetch(source: ModelSource, directory: String): Option[ModelMetadata] = {
    try {
      val pbFile = source.getReadableFile(s"$directory/saved_model.pb")
      val savedModel = SavedModel.parseFrom(Files.newInputStream(pbFile.toPath))
      val signatures = savedModel
        .getMetaGraphsList
        .flatMap{ metagraph =>
          metagraph.getSignatureDefMap.map {
            case (_, signatureDef) =>
              convertSignature(signatureDef)
          }.toList
        }

      Some(
        ModelMetadata(
          modelName = directory,
          contract = ModelContract(
            directory,
            signatures
          ),
          modelType = ModelType.Tensorflow()
        )
      )
    } catch {
      case e: NoSuchFileException =>
        logger.debug(s"$directory in not a valid Tensorflow model")
        None
      case e: FileNotFoundException =>
        logger.debug(s"$source $directory in not a valid Tensorflow model")
        None
    }
  }

  private def convertTensor(tensorInfo: TFTensorInfo): TensorInfo = {
    val shape = if (tensorInfo.hasTensorShape) {
      val tShape = tensorInfo.getTensorShape
      Some(TensorShapeProto(tShape.getDimList.map(x => TensorShapeProto.Dim(x.getSize, x.getName)), tShape.getUnknownRank))
    } else None
    val convertedDtype = DataType.fromValue(tensorInfo.getDtypeValue)
    TensorInfo(
      dtype = convertedDtype,
      tensorShape = shape
    )
  }

  private def convertTensorMap(tensorMap: Map[String, TFTensorInfo]): List[ModelField] = {
    tensorMap.map {
      case (inputName, inputDef) =>
        val convertedInputDef = convertTensor(inputDef)
        val tensorInfo = ModelField.InfoOrSubfields.Info(
          TensorInfo(
            convertedInputDef.dtype,
            convertedInputDef.tensorShape
          )
        )
        ModelField(
          fieldName = inputName,
          infoOrSubfields = tensorInfo
        )
    }.toList
  }

  private def convertSignature(signatureDef: SignatureDef): ModelSignature = {
    ModelSignature(
      signatureName = signatureDef.getMethodName,
      inputs = convertTensorMap(signatureDef.getInputsMap.toMap),
      outputs = convertTensorMap(signatureDef.getInputsMap.toMap)
    )
  }

}

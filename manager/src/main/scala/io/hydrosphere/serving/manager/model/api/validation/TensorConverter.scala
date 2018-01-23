package io.hydrosphere.serving.manager.model.api.validation

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_field.ModelField.InfoOrSubfields.{Empty, Info, Subfields}
import io.hydrosphere.serving.manager.service.JsonPredictRequest
import io.hydrosphere.serving.tensorflow.api.predict.PredictRequest
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.types.DataType
import spray.json._

class PredictRequestContractValidator(val contract: ModelContract) {

  def convert(data: JsonPredictRequest): Either[ValidationError, PredictRequest] = {
    contract.signatures.find(_.signatureName == data.signatureName) match {
      case Some(signature) =>
        // rootField is a virtual field that aggregates all request inputs
        val rootField = ModelField(
          "root",
          ModelField.InfoOrSubfields.Subfields(
            ModelField.ComplexField(
              signature.inputs
            )
          )
        )

        val fieldValidator = new ComplexFieldValidator(rootField, signature.inputs)
        fieldValidator.convert(data.inputs) match {
          case Left(errors) =>
            Left(new SignatureValidationError(errors, signature))
          case Right(tensor) =>
            Right(
              PredictRequest(
                modelSpec = Some(data.toModelSpec),
                inputs = tensor.mapVal
              )
            )
        }

      case None => Left(new SignatureMissingError(data.signatureName, contract))
    }
  }

}

class ModelFieldValidator(val modelField: ModelField) {

  def convert(data: JsValue): Either[ValidationError, TensorProto] = {
    modelField.infoOrSubfields match {
      case Empty => ??? // wtf is empty field ?

      case Subfields(subfields) =>
        val complexFieldValidator = new ComplexFieldValidator(modelField, subfields.data)
        complexFieldValidator.convert(data).left.map { errors =>
          new ComplexFieldValidationError(errors, modelField)
        }

      case Info(value) =>
        val infoFieldValidator = new InfoFieldValidator(modelField, value)
        infoFieldValidator.convert(data)
    }
  }

}

class ComplexFieldValidator(val modelField: ModelField, val subfields: Seq[ModelField]) {

  def convert(data: JsValue): Either[Seq[ValidationError], TensorProto] = {
    data match {
      case obj: JsObject =>
        val a = subfields.map { field =>
          obj.getFields(field.fieldName).headOption match {
            case None => Left(new FieldMissingError(field.fieldName))

            case Some(fieldData) =>
              val fieldValidator = new ModelFieldValidator(field)
              fieldValidator.convert(fieldData).right.map { tensor =>
                field.fieldName -> tensor
              }
          }
        }
        if (a.exists(_.isLeft)) {
          val errors = a.collect { case Left(err) => err }
          Left(errors)
        } else {
          val tensors = a.collect { case Right((name, tensor)) => name -> tensor }.toMap
          Right(
            TensorProto(
              dtype = DataType.DT_MAP,
              mapVal = tensors
            )
          )
        }

      case _ => Left(Seq(new IncompatibleFieldTypeError(modelField.fieldName, DataType.DT_MAP)))
    }
  }

}

class InfoFieldValidator(val field: ModelField, val tensorInfo: TensorInfo) {

  def convert(data: JsValue): Either[ValidationError, TensorProto] = {
    data match {
      // collection
      case arr: JsArray => arrayToTensor(arr, tensorInfo)
      // scalar
      case JsString(value) => toTensor(Seq(value), tensorInfo)
      case JsNumber(value) => toTensor(Seq(value.doubleValue()), tensorInfo)
      case bool: JsBoolean => toTensor(Seq(bool), tensorInfo)
      // invalid
      case other => Left(new IncompatibleFieldTypeError(field.fieldName, tensorInfo.dtype))
    }
  }

  def arrayToTensor(data: JsArray, tensorInfo: TensorInfo): Either[ValidationError, TensorProto] = {
    val reshapedData = tensorInfo.tensorShape match {
      case Some(_) => flatten(data)
      case None => List(data)
    }

    toTensor(reshapedData, tensorInfo)
  }

  def toTensor(flatData: Seq[Any], tensorInfo: TensorInfo): Either[ValidationError, TensorProto] = {
    TypedTensor.constructTensor(flatData, tensorInfo)
  }

  private def flatten(arr: JsArray): List[Any] = {
    arr.elements.flatMap {
      case arr: JsArray => flatten(arr)
    }.toList
  }

}
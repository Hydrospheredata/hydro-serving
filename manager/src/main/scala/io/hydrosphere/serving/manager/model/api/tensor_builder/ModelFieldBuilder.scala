package io.hydrosphere.serving.manager.model.api.tensor_builder

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_field.ModelField.TypeOrSubfields._
import io.hydrosphere.serving.manager.model.api.validation.{ComplexFieldValidationError, ValidationError}
import io.hydrosphere.serving.tensorflow.tensor.TypedTensor
import spray.json.JsValue

class ModelFieldBuilder(val modelField: ModelField) {

  def convert(data: JsValue): Either[ValidationError, TypedTensor[_]] = {
    modelField.typeOrSubfields match {
      case Empty => Left(new ValidationError(s"$modelField is empty") {})

      case Subfields(subfields) =>
        val complexFieldValidator = new ComplexFieldBuilder(modelField, subfields.data)
        complexFieldValidator.convert(data).left.map { errors =>
          new ComplexFieldValidationError(errors, modelField)
        }

      case Dtype(value) =>
        val infoFieldValidator = new InfoFieldBuilder(modelField, value)
        infoFieldValidator.convert(data)
    }
  }

}

package io.hydrosphere.serving.manager.model.api.tensor_builder

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.validation.{SignatureValidationError, ValidationError}
import io.hydrosphere.serving.tensorflow.tensor.TypedTensor
import spray.json.JsObject

class SignatureBuilder(val signature: ModelSignature) {
  def convert(data: JsObject): Either[ValidationError, Map[String, TypedTensor[_]]] = {
    // rootField is a virtual field that aggregates all request inputs
    val rootField = ModelField(
      "root",
      None,
      ModelField.TypeOrSubfields.Subfields(
        ModelField.Subfield(
          signature.inputs
        )
      )
    )

    val fieldValidator = new ComplexFieldBuilder(rootField, signature.inputs)
    fieldValidator.convert(data) match {
      case Left(errors) =>
        Left(new SignatureValidationError(errors, signature))
      case Right(tensor) =>
        Right(tensor.data.head)
    }
  }
}

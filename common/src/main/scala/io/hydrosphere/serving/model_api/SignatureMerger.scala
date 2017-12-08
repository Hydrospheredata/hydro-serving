package io.hydrosphere.serving.model_api

import hydroserving.contract.model_field.ModelField
import hydroserving.contract.model_signature.ModelSignature

object SignatureMerger {

  def merge(inputs: Seq[ModelField], inputs1: Seq[ModelField]): Seq[ModelField] = {
    inputs.zip(inputs1).flatMap {
      case (in1, in2) =>
        if (in1.fieldName == in2.fieldName) {
          if (SignatureChecker.areCompatible(in1, in2)) {
            List(in1)
          } else {
            throw new IllegalArgumentException(s"$in1 and $in2 aren't compatible")
          }
        } else {
          List(in1, in2)
        }
    }
  }

  def merge(signature1: ModelSignature, signature2: ModelSignature): ModelSignature = {
    val mergedIns = merge(signature1.inputs, signature2.inputs)
    val mergedOuts = merge(signature1.outputs, signature2.outputs)
    ModelSignature(
      s"${signature1.signatureName}&${signature2.signatureName}",
      mergedIns,
      mergedOuts
    )
  }
}

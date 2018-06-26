package io.hydrosphere.serving.manager.model.api.ops

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.api.description.SignatureDescription

trait ModelSignatureOps {
  def merge(signature1: ModelSignature, signature2: ModelSignature): ModelSignature = {
    val mergedIns = ModelFieldOps.mergeAll(signature1.inputs, signature2.inputs)
    val mergedOuts = ModelFieldOps.mergeAll(signature1.outputs, signature2.outputs)
    ModelSignature(
      s"${signature1.signatureName}&${signature2.signatureName}",
      mergedIns,
      mergedOuts
    )
  }

  def flatten(modelSignature: ModelSignature): SignatureDescription = {
    val inputs = ModelFieldOps.flatten(modelSignature.inputs)
    val outputs = ModelFieldOps.flatten(modelSignature.outputs)
    SignatureDescription(modelSignature.signatureName, inputs, outputs)
  }

  def append(head: ModelSignature, tail: ModelSignature): Option[ModelSignature] = {
    if (tail.inputs.isEmpty) {
      None
    } else {
      val maybeFields: Option[Seq[ModelField]] = ModelFieldOps.appendAll(head.outputs, tail.inputs)
      maybeFields.map { _ =>
        ModelSignature(
          s"${head.signatureName}>${tail.signatureName}",
          head.inputs,
          tail.outputs
        )
      }
    }
  }
}

object ModelSignatureOps extends ModelSignatureOps
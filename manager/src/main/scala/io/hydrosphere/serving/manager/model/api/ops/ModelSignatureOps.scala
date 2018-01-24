package io.hydrosphere.serving.manager.model.api.ops

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.api.description.SignatureDescription

trait ModelSignatureOps {

  implicit class ModelSignaturePumped(modelSignature: ModelSignature) {
    def +++(other: ModelSignature): ModelSignature = {
      ModelSignatureOps.merge(modelSignature, other)
    }
  }

}

object ModelSignatureOps {
  def merge(signature1: ModelSignature, signature2: ModelSignature): ModelSignature = {
    val mergedIns = ModelFieldOps.merge(signature1.inputs, signature2.inputs)
    val mergedOuts = ModelFieldOps.merge(signature1.outputs, signature2.outputs)
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
}


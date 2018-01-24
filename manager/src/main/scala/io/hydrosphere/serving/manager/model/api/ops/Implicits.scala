package io.hydrosphere.serving.manager.model.api.ops

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_field.ModelField.InfoOrSubfields.Subfields
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.api.ContractBuilders
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import spray.json.JsValue

trait Implicits extends ModelContractOps with ModelSignatureOps
  with ModelFieldOps with TensorShapeProtoOps with TensorProtoOps {


}

object Implicits extends Implicits
package io.hydrosphere.serving.manager.model.api.ops

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.api.description.{ContractDescription, SignatureDescription}

trait ModelContractOps {
  implicit class ModelContractPumped(modelContract: ModelContract) {
    def flatten: ContractDescription = {
      ContractDescription(
        ModelContractOps.flatten(modelContract)
      )
    }
  }
}

object ModelContractOps {
  def flatten(modelContract: ModelContract): List[SignatureDescription] = {
    modelContract.signatures.map(ModelSignatureOps.flatten).toList
  }
}

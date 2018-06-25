package io.hydrosphere.serving.manager.model.api.ops

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.api.description.ContractDescription

trait ModelContractOps {

  implicit class ModelContractPumped(modelContract: ModelContract) {
    def flatten: ContractDescription = {
      ModelContractOps.flatten(modelContract)
    }
  }

  def flatten(modelContract: ModelContract): ContractDescription = {
    ContractDescription(
      modelContract.signatures.map(ModelSignatureOps.flatten).toList
    )
  }
}

object ModelContractOps extends ModelContractOps
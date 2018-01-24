package io.hydrosphere.serving.manager.model.api.description

import io.hydrosphere.serving.contract.model_contract.ModelContract


case class ContractDescription(
  signatures: List[SignatureDescription]
) {
  def toContract: ModelContract = ContractDescription.toContract(this)
}

object ContractDescription {
  def toContract(contractDescription: ContractDescription): ModelContract = {
    ModelContract(
      modelName = "",
      signatures = contractDescription.signatures.map(SignatureDescription.toSignature)
    )
  }
}


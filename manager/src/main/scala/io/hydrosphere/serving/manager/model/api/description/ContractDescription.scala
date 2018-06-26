package io.hydrosphere.serving.manager.model.api.description

import io.hydrosphere.serving.contract.model_contract.ModelContract

case class ContractDescription(
  signatures: Seq[SignatureDescription]
) {
  def toContract: ModelContract = ContractDescription.toContract(this)
}

object ContractDescription {
  def toContract(contractDescription: ContractDescription): ModelContract = {
    ModelContract(
      signatures = contractDescription.signatures.map(SignatureDescription.toSignature)
    )
  }
}
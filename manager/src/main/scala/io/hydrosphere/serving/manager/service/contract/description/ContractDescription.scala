package io.hydrosphere.serving.manager.service.contract.description

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.util.CommonJsonSupport._


case class ContractDescription(
  signatures: List[SignatureDescription]
) {
  def toContract: ModelContract = ContractDescription.toContract(this)
}

object ContractDescription {
  implicit val contractDescFormat = jsonFormat1(ContractDescription.apply)

  def toContract(contractDescription: ContractDescription): ModelContract = {
    ModelContract(
      modelName = "",
      signatures = contractDescription.signatures.map(SignatureDescription.toSignature)
    )
  }
}


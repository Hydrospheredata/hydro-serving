package io.hydrosphere.serving.manager.service.contract

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.HResult
import spray.json.JsObject

trait ContractUtilityService {
  def generatePayload(contract: ModelContract, signature: String): HResult[JsObject]
}

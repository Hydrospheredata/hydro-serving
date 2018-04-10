package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.DataGenerator
import io.hydrosphere.serving.manager.model.api.json.TensorJsonLens
import io.hydrosphere.serving.manager.model.{HResult, Result}
import spray.json.JsObject

trait ContractUtilityService {
  def generatePayload(contract: ModelContract, signature: String): HResult[JsObject]
}

class ContractUtilityServiceImpl extends ContractUtilityService {
  def generatePayload(contract: ModelContract, signature: String): HResult[JsObject] = {
    DataGenerator.forContract(contract, signature) match {
      case Some(generator) => Result.ok(TensorJsonLens.mapToJson(generator.generateInputs))
      case None => Result.clientError(s"Can't find '$signature' signature in contract")
    }
  }
}
package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.manager.model.api.validation.PredictRequestContractValidator
import org.scalatest.WordSpec

class ValidationSpecs extends WordSpec {
  classOf[PredictRequestContractValidator].getSimpleName should {
    "convert" when {
      "flat json is compatible with contract" in {pending}
      "nested json is compatible with contract" in {pending}
    }

    "fail" when {
      "flat json is incompatible with contract" in {pending}
      "nested json is incompatible with contract" in {pending}
    }
  }
}

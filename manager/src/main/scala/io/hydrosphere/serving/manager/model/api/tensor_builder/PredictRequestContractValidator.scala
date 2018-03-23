package io.hydrosphere.serving.manager.model.api.tensor_builder

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.validation.{SignatureMissingError, ValidationError}
import io.hydrosphere.serving.manager.service.JsonPredictRequest
import io.hydrosphere.serving.tensorflow.api.predict.PredictRequest

class PredictRequestContractValidator(val contract: ModelContract) {

  def convert(data: JsonPredictRequest): Either[ValidationError, PredictRequest] = {
    contract.signatures.find(_.signatureName == data.signatureName) match {
      case Some(signature) =>
        val validator = new SignatureBuilder(signature)
        validator.convert(data.inputs).right.map { tensors =>
          PredictRequest(
            modelSpec = Some(data.toModelSpec),
            inputs = tensors.mapValues(_.toProto)
          )
        }

      case None => Left(new SignatureMissingError(data.signatureName, contract))
    }
  }

}

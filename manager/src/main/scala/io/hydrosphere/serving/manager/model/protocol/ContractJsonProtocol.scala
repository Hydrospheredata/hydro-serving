package io.hydrosphere.serving.manager.model.protocol

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.api.description.{ContractDescription, FieldDescription, SignatureDescription}
import io.hydrosphere.serving.monitoring.data_profile_types.DataProfileType
import io.hydrosphere.serving.tensorflow.types.DataType

trait ContractJsonProtocol extends CommonJsonProtocol {
  implicit val dataProfileTypeFormat = protoEnumFormat(DataProfileType)

  implicit val dataTypeFormat = protoEnumFormat(DataType)

  implicit val fieldDescFormat = jsonFormat3(FieldDescription)
  implicit val sigDescFormat = jsonFormat3(SignatureDescription.apply)
  implicit val contractDescFormat = jsonFormat1(ContractDescription.apply)

  implicit val modelSignatureFormat = protoFormat(ModelSignature)
  implicit val modelContractFormat = protoFormat(ModelContract)
}

object ContractJsonProtocol extends ContractJsonProtocol

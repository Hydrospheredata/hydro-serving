package io.hydrosphere.serving.manager.model.protocol

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.description.{ContractDescription, FieldDescription, SignatureDescription}
import io.hydrosphere.serving.tensorflow.types.DataType
import spray.json.{DeserializationException, JsString, JsValue, JsonFormat}

trait ContractJsonProtocol extends CommonJsonProtocol {
  implicit val dataTypeFormat = new JsonFormat[DataType] {
    override def read(json: JsValue) = {
      json match {
        case JsString(str) => DataType.fromName(str).getOrElse(throw new IllegalArgumentException(s"$str is invalid DataType"))
        case x => throw DeserializationException(s"$x is not a correct DataType")
      }
    }

    override def write(obj: DataType) = {
      JsString(obj.toString())
    }
  }

  implicit val fieldDescFormat = jsonFormat3(FieldDescription)
  implicit val sigDescFormat = jsonFormat3(SignatureDescription.apply)
  implicit val contractDescFormat = jsonFormat1(ContractDescription.apply)

  implicit val modelContractFormat = new JsonFormat[ModelContract] {
    override def read(json: JsValue) = {
      json match {
        case JsString(str) => ModelContract.fromAscii(str)
        case x => throw DeserializationException(s"$x is not a correct ModelContract message")
      }
    }

    override def write(obj: ModelContract) = {
      JsString(obj.toString)
    }
  }

  implicit val modelSignatureFormat = new JsonFormat[ModelSignature] {
    override def read(json: JsValue) = {
      json match {
        case JsString(str) => ModelSignature.fromAscii(str)
        case x => throw DeserializationException(s"$x is not a correct ModelSignature message")
      }
    }

    override def write(obj: ModelSignature) = {
      JsString(obj.toString)
    }
  }

}

object ContractJsonProtocol extends ContractJsonProtocol

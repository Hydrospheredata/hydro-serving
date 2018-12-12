package io.hydrosphere.serving.manager.infrastructure.protocol

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.model.api.description.{ContractDescription, FieldDescription, SignatureDescription}
import io.hydrosphere.serving.monitoring.data_profile_types.DataProfileType
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import spray.json._

import scala.collection.mutable

trait ContractJsonProtocol extends CommonJsonProtocol {
  implicit val dataProfileTypeFormat = protoEnumFormat(DataProfileType)

  implicit val dataTypeFormat = protoEnumFormat(DataType)

  implicit val fieldDescFormat = jsonFormat3(FieldDescription)
  implicit val sigDescFormat = jsonFormat3(SignatureDescription.apply)
  implicit val contractDescFormat = jsonFormat1(ContractDescription.apply)

  implicit val tensorShapeDimFormat = jsonFormat2(TensorShapeProto.Dim.apply)
  implicit val tensorShapeFormat = jsonFormat2(TensorShapeProto.apply)

  implicit val modelFieldFormat = new RootJsonFormat[ModelField] {
    override def read(json: JsValue): ModelField = ???

    override def write(obj: ModelField): JsValue = {
      val fields = new mutable.HashMap[String, JsValue]()
      fields += "name" -> JsString(obj.name)
      fields += "shape" -> obj.shape.map(_.toJson).getOrElse(JsNull)

      obj.typeOrSubfields match {
        case ModelField.TypeOrSubfields.Dtype(value) =>
          fields += "dtype" -> JsString(value.name)
        case ModelField.TypeOrSubfields.Subfields(value) =>
          fields += "subfields" -> JsArray(value.data.map(write).toVector)
        case ModelField.TypeOrSubfields.Empty =>
          fields += "dtype" -> JsNull
      }
      JsObject(fields.toMap)
    }
  }

  implicit val modelSignatureFormat = jsonFormat3(ModelSignature.apply)
  implicit val modelContractFormat = jsonFormat2(ModelContract.apply)
}

object ContractJsonProtocol extends ContractJsonProtocol

package io.hydrosphere.serving.manager.infrastructure.protocol

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.model.api.description.{ContractDescription, FieldDescription, SignatureDescription}
import io.hydrosphere.serving.manager.data_profile_types.DataProfileType
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

    object DtypeJson {
      def unapply(arg: JsValue): Option[(JsString, Option[JsObject], JsString)] = {
        arg match {
          case JsObject(fields) =>
            for {
              name <- fields.get("name")
              dtype <- fields.get("dtype")
            } yield (
              name.asInstanceOf[JsString],
              fields.get("shape").map(_.asInstanceOf[JsObject]),
              dtype.asInstanceOf[JsString]
            )
          case _ => None
        }
      }
    }

    object SubfieldsJson {
      def unapply(arg: JsValue): Option[(JsString, Option[JsObject], JsArray)] = {
        arg match {
          case JsObject(fields) =>
            for {
              name <- fields.get("name")
              subfields <- fields.get("subfields")
            } yield (
              name.asInstanceOf[JsString],
              fields.get("shape").map(_.asInstanceOf[JsObject]),
              subfields.asInstanceOf[JsArray]
            )
          case _ => None
        }
      }
    }

    override def read(json: JsValue): ModelField = json match {
      case DtypeJson(name, shape, dtype) =>
        ModelField(name.value, shape.map(_.convertTo[TensorShapeProto]), ModelField.TypeOrSubfields.Dtype(DataType.fromName(dtype.value).get))

      case SubfieldsJson(name, shape, subs) =>
        val subfields = ModelField.TypeOrSubfields.Subfields(
          ModelField.Subfield(subs.elements.map(read))
        )
        ModelField(name.value, shape.map(_.convertTo[TensorShapeProto]), subfields)

      case x => throw DeserializationException(s"Invalid ModelField: $x")
    }

    override def write(obj: ModelField): JsValue = {
      val fields = new mutable.HashMap[String, JsValue]()
      fields += "name" -> JsString(obj.name)
      obj.shape.foreach { shape =>
        fields += "shape" -> shape.toJson
      }
      obj.typeOrSubfields match {
        case ModelField.TypeOrSubfields.Dtype(value) =>
          fields += "dtype" -> JsString(value.name)
        case ModelField.TypeOrSubfields.Subfields(value) =>
          fields += "subfields" -> JsArray(value.data.map(write).toVector)
        case ModelField.TypeOrSubfields.Empty => fields
      }
      JsObject(fields.toMap)
    }
  }

  implicit val modelSignatureFormat = jsonFormat3(ModelSignature.apply)
  implicit val modelContractFormat = jsonFormat2(ModelContract.apply)
}

object ContractJsonProtocol extends ContractJsonProtocol

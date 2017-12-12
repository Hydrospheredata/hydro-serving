package io.hydrosphere.serving.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import hydroserving.contract.model_contract.ModelContract
import hydroserving.tensorflow.types.DataType
import hydroserving.tensorflow.types.DataType.{DT_BFLOAT16, DT_BFLOAT16_REF, DT_BOOL, DT_BOOL_REF, DT_COMPLEX128, DT_COMPLEX128_REF, DT_COMPLEX64, DT_COMPLEX64_REF, DT_DOUBLE, DT_DOUBLE_REF, DT_FLOAT, DT_FLOAT_REF, DT_HALF, DT_HALF_REF, DT_INT16, DT_INT16_REF, DT_INT32, DT_INT32_REF, DT_INT64, DT_INT64_REF, DT_INT8, DT_INT8_REF, DT_INVALID, DT_QINT16, DT_QINT16_REF, DT_QINT32, DT_QINT32_REF, DT_QINT8, DT_QINT8_REF, DT_QUINT16, DT_QUINT16_REF, DT_QUINT8, DT_QUINT8_REF, DT_RESOURCE, DT_RESOURCE_REF, DT_STRING, DT_STRING_REF, DT_UINT16, DT_UINT16_REF, DT_UINT32, DT_UINT32_REF, DT_UINT64, DT_UINT64_REF, DT_UINT8, DT_UINT8_REF, DT_VARIANT, DT_VARIANT_REF, Unrecognized}
import io.hydrosphere.serving.model_api.ContractOps.{FieldDescription, SignatureDescription}
import io.hydrosphere.serving.model_api.ModelType
import org.apache.logging.log4j.scala.Logging
import spray.json._

/**
  *
  */
class EnumJsonConverter[T <: scala.Enumeration](enu: T) extends RootJsonFormat[T#Value] {
  override def write(obj: T#Value): JsValue = JsString(obj.toString)

  override def read(json: JsValue): T#Value = {
    json match {
      case JsString(txt) => enu.withName(txt)
      case somethingElse => throw DeserializationException(s"Expected a value from enum $enu instead of $somethingElse")
    }
  }
}

trait CommonJsonSupport extends SprayJsonSupport with DefaultJsonProtocol with Logging {

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(any: Any): JsValue = any match {
      case n: Int => JsNumber(n)
      case n: Long => JsNumber(n)
      case n: Float => JsNumber(n)
      case n: Double => JsNumber(n)
      case n: BigDecimal => JsNumber(n)
      case s: String => JsString(s)
      case b: Boolean => JsBoolean(b)
      case list: List[_] => seqFormat[Any].write(list)
      case array: Array[_] => seqFormat[Any].write(array.toList)
      case map: Map[String, _]@unchecked => mapFormat[String, Any] write map
      case e => logger.error(s"${e.toString}"); throw DeserializationException(e.toString)
    }

    def read(value: JsValue): Any = value match {
      case JsNumber(n) => n.toDouble
      case JsString(s) => s
      case JsBoolean(b) => b
      case _: JsArray => listFormat[Any].read(value)
      case _: JsObject => mapFormat[String, Any].read(value)
      case e => throw DeserializationException(e.toString)
    }
  }

  implicit val localDateTimeFormat = new JsonFormat[LocalDateTime] {
    def write(x: LocalDateTime) = JsString(DateTimeFormatter.ISO_DATE_TIME.format(x))

    def read(value: JsValue) = value match {
      case JsString(x) => LocalDateTime.parse(x, DateTimeFormatter.ISO_DATE_TIME)
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse LocalDateTime")
    }
  }

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

  implicit val modelTypeFormat = new JsonFormat[ModelType] {
    override def read(json: JsValue) = {
      json match {
        case JsString(str) => ModelType.fromTag(str)
        case x => throw DeserializationException(s"$x is not a valid ModelType")
      }
    }

    override def write(obj: ModelType) = {
      JsString(obj.toTag)
    }
  }

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
  implicit val fieldDescFormat = jsonFormat3(FieldDescription.apply)
  implicit val sigDescFormat = jsonFormat3(SignatureDescription.apply)

  implicit val runtimeTypeFormat = jsonFormat6(RuntimeType)
  implicit val modelRuntimeFormat = jsonFormat13(ModelRuntime)
  implicit val servingEnvironmentFormat = jsonFormat3(ServingEnvironment)
  implicit val modelServiceFormat = jsonFormat8(ModelService)

  implicit val errorResponseFormat = jsonFormat1(ErrorResponse)
  implicit val serviceWeightFormat = jsonFormat3(ServiceWeight)
  implicit val applicationStageFormat = jsonFormat1(ApplicationStage)
  implicit val applicationExecutionGraphFormat = jsonFormat1(ApplicationExecutionGraph)
  implicit val applicationFormat = jsonFormat4(Application)
}

package io.hydrosphere.serving.manager.controller

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ContractOps.{FieldDescription, SignatureDescription}
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.tensorflow.types.DataType
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
      case n: JsValue => n
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
      case e => throw DeserializationException(e.toString)
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

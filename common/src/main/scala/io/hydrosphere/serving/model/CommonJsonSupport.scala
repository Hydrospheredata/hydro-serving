package io.hydrosphere.serving.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.hydrosphere.serving.model_api._
import org.apache.commons.lang3.SerializationException
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

  implicit object ScalarFieldFormat extends RootJsonFormat[ScalarField] {
    override def read(json: JsValue): ScalarField = json match {
      case JsString("integer") => FInteger
      case JsString("double") => FDouble
      case JsString("string") => FString
      case JsString("any_scalar") => FAnyScalar
      case _ => throw DeserializationException(s"$json is not a valid scalar type definition.")
    }

    override def write(obj: ScalarField): JsValue = obj match {
      case FInteger => JsString("integer")
      case FDouble => JsString("double")
      case FString => JsString("string")
      case FAnyScalar => JsString("any_scalar")
    }
  }

  implicit val matrixFormat = jsonFormat2(FMatrix.apply)

  implicit object FieldTypeFormat extends RootJsonFormat[FieldType] {
    override def read(json: JsValue): FieldType = json match {
      case JsObject(field) if field.get("type").isDefined && field("type") == JsString("matrix") =>
        FMatrix(field("item_type").convertTo[ScalarField], field("shape").convertTo[List[Long]])
      case JsString("any") => FAny
      case x => ScalarFieldFormat.read(x)
    }

    override def write(obj: FieldType): JsValue = obj match {
      case FMatrix(fType, shape) =>
        val s = Map(
          "shape" -> JsArray(shape.map(JsNumber(_)).toVector),
          "item_type" -> fType.toJson,
          "type" -> JsString("matrix")
        )
        JsObject(s)
      case FAny => JsString("any")
      case x: ScalarField => ScalarFieldFormat.write(x)
    }
  }


  implicit val typedFieldFormat = jsonFormat2(ModelField.apply)

  implicit val dataFrameFormat = jsonFormat1(DataFrame.apply)

  implicit object ModelApiFormat extends RootJsonFormat[ModelApi] {
    override def read(json: JsValue): ModelApi = json match {
      case x: JsObject if x.fields.isEmpty => UntypedAPI
      case x: JsObject => x.convertTo[DataFrame]
      case value => throw new SerializationException(s"Incorrect JSON for model api definition: $value")
    }

    override def write(obj: ModelApi): JsValue = obj match {
      case x: DataFrame => x.toJson
      case x: UntypedAPI.type => JsObject.empty
      case value => throw DeserializationException(s"$value is not a valid model api definition.")
    }
  }

  implicit val runtimeTypeFormat = jsonFormat5(RuntimeType)
  implicit val modelRuntimeFormat = jsonFormat14(ModelRuntime)
  implicit val servingEnvironmentFormat = jsonFormat3(ServingEnvironment)
  implicit val modelServiceFormat = jsonFormat8(ModelService)

  implicit val errorResponseFormat = jsonFormat1(ErrorResponse)
  implicit val serviceWeightFormat = jsonFormat2(ServiceWeight)
  implicit val applicationStageFormat = jsonFormat1(ApplicationStage)
  implicit val applicationExecutionGraphFormat = jsonFormat1(ApplicationExecutionGraph)
  implicit val applicationFormat = jsonFormat4(Application)
}

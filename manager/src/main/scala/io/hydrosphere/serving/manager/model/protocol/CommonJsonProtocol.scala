package io.hydrosphere.serving.manager.model.protocol

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.language.reflectiveCalls

trait CommonJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol with Logging {

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

  implicit val uuidFormat = new RootJsonFormat[UUID] {
    override def write(obj: UUID): JsValue = JsString(obj.toString)

    override def read(json: JsValue): UUID = {
      json match {
        case JsString(str) => UUID.fromString(str)
        case x => throw DeserializationException(s"Invalid JsValue for UUID. Expected string, got $x")
      }
    }
  }

  implicit val throwableWriter = new RootJsonFormat[Throwable] {
    override def write(obj: Throwable): JsValue = JsString(obj.getMessage)

    override def read(json: JsValue): Throwable = throw DeserializationException("Can't deserealize exceptions")
  }

  implicit def enumFormat[T <: scala.Enumeration](enum: T) = new RootJsonFormat[T#Value] {
    override def write(obj: T#Value): JsValue = JsString(obj.toString)

    override def read(json: JsValue): T#Value = json match {
      case JsString(txt) => enum.withName(txt)
      case somethingElse => throw DeserializationException(s"Expected a value from enum $enum instead of $somethingElse")
    }
  }

  implicit val serviceTaskStatusFormat = enumFormat(ServiceTaskStatus)

  implicit val localDateTimeFormat = new JsonFormat[LocalDateTime] {
    def write(x: LocalDateTime) = JsString(DateTimeFormatter.ISO_DATE_TIME.format(x))

    def read(value: JsValue) = value match {
      case JsString(x) => LocalDateTime.parse(x, DateTimeFormatter.ISO_DATE_TIME)
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse LocalDateTime")
    }
  }
}

object CommonJsonProtocol extends CommonJsonProtocol
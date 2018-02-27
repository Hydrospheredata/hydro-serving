package io.hydrosphere.serving.manager.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.api.description._
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.clouddriver.{MetricServiceTargetLabels, MetricServiceTargets}
import io.hydrosphere.serving.tensorflow.types.DataType
import org.apache.logging.log4j.scala.Logging
import spray.json._

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

  implicit def enumFormat[T <: scala.Enumeration](enum: T) = new RootJsonFormat[T#Value] {
    override def write(obj: T#Value): JsValue = JsString(obj.toString)

    override def read(json: JsValue): T#Value = json match {
      case JsString(txt) => enum.withName(txt)
      case somethingElse => throw DeserializationException(s"Expected a value from enum $enum instead of $somethingElse")
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

  implicit val modelBuildStatusFormat = enumFormat(ModelBuildStatus)


  implicit val fieldDescFormat = jsonFormat3(FieldDescription)
  implicit val sigDescFormat = jsonFormat3(SignatureDescription.apply)
  implicit val contractDescFormat = jsonFormat1(ContractDescription.apply)

  implicit val modelFormat = jsonFormat8(Model)
  implicit val runtimeFormat = jsonFormat6(Runtime)
  implicit val modelVersionFormat = jsonFormat11(ModelVersion)
  implicit val environmentFormat = jsonFormat3(Environment)
  implicit val serviceFormat = jsonFormat8(Service)
  implicit val modelBuildFormat = jsonFormat9(ModelBuild)

  implicit val errorResponseFormat = jsonFormat1(ErrorResponse)

  implicit val serviceKeyDescriptionFormat = jsonFormat3(ServiceKeyDescription.apply)
  implicit val serviceWeightFormat = jsonFormat3(WeightedService)
  implicit val applicationStageFormat = jsonFormat2(ApplicationStage.apply)
  implicit val applicationExecutionGraphFormat = jsonFormat1(ApplicationExecutionGraph)
  implicit val applicationKafkaStreamingFormat = jsonFormat4(ApplicationKafkaStream)
  implicit val applicationFormat = jsonFormat5(Application)

  implicit val modelServiceInstanceStatusFormat = enumFormat(ServiceInstanceStatus)

  implicit val createServiceRequest = jsonFormat5(CreateServiceRequest)

  implicit val createRuntimeRequest = jsonFormat5(CreateRuntimeRequest)

  implicit val createOrUpdateModelRequest = jsonFormat6(CreateOrUpdateModelRequest)

  implicit val createModelVersionRequest = jsonFormat12(CreateModelVersionRequest)

  implicit val awsAuthFormat = jsonFormat2(AWSAuthKeys)

  implicit val localSourceParamsFormat = jsonFormat1(LocalSourceParams)

  implicit val s3SourceParamsFormat = jsonFormat4(S3SourceParams)

  implicit val sourceParamsFormat = new JsonFormat[SourceParams] {
    override def read(json: JsValue) = {
      json match {
        case JsObject(fields) if fields.isDefinedAt("path") =>
          LocalSourceParams(fields("path").convertTo[String])
        case JsObject(fields) if fields.isDefinedAt("queueName") && fields.isDefinedAt("bucketName") =>
          S3SourceParams(
            awsAuth = fields.get("awsAuth").map(_.convertTo[AWSAuthKeys]),
            bucketName = fields("bucketName").convertTo[String],
            queueName = fields("queueName").convertTo[String],
            region = fields("region").convertTo[String]
          )
      }
    }

    override def write(obj: SourceParams) = {
      obj match {
        case x: LocalSourceParams => x.toJson
        case x: S3SourceParams => x.toJson
        case _ => ???
      }
    }
  }

  implicit val modelSourceConfigFormat = jsonFormat3(ModelSourceConfigAux)

  implicit val createEnvironmentRequest = jsonFormat2(CreateEnvironmentRequest)

  implicit val aggregatedModelInfoFormat=jsonFormat4(AggregatedModelInfo)

  implicit val metricServiceTargetLabelsFormat = jsonFormat11(MetricServiceTargetLabels)
  implicit val metricServiceTargetsFormat = jsonFormat2(MetricServiceTargets)
}

object CommonJsonSupport extends CommonJsonSupport
package io.hydrosphere.serving.manager.model.protocol

import io.hydrosphere.serving.manager.controller.environment.CreateEnvironmentRequest
import io.hydrosphere.serving.manager.controller.model.AddModelRequest
import io.hydrosphere.serving.manager.controller.runtime.CreateRuntimeRequest
import io.hydrosphere.serving.manager.model.ModelBuildStatus
import io.hydrosphere.serving.manager.model.Result.{ClientError, ErrorCollection, HError, InternalError}
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Model
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.aggregated_info.AggregatedModelInfo
import io.hydrosphere.serving.manager.service.clouddriver.{MetricServiceTargetLabels, MetricServiceTargets}
import io.hydrosphere.serving.manager.service.model._
import io.hydrosphere.serving.manager.service.model_version.CreateModelVersionRequest
import io.hydrosphere.serving.manager.service.service.CreateServiceRequest
import spray.json.{DeserializationException, JsObject, JsString, JsValue, JsonFormat}
import spray.json._

trait CompleteJsonProtocol extends CommonJsonProtocol with ContractJsonProtocol with ModelJsonProtocol {
  implicit val createServiceRequest = jsonFormat5(CreateServiceRequest)

  implicit val createRuntimeRequest = jsonFormat5(CreateRuntimeRequest)

  implicit val createOrUpdateModelRequest = jsonFormat6(CreateOrUpdateModelRequest)

  implicit val createModelVersionRequest = jsonFormat12(CreateModelVersionRequest)

  implicit val createEnvironmentRequest = jsonFormat2(CreateEnvironmentRequest)

  implicit val addModelFormat = jsonFormat2(AddModelRequest.apply)

  implicit val aggregatedModelInfoFormat=jsonFormat5(AggregatedModelInfo)

  implicit val metricServiceTargetLabelsFormat = jsonFormat11(MetricServiceTargetLabels)

  implicit val metricServiceTargetsFormat = jsonFormat2(MetricServiceTargets)

  implicit val modelUpdatedFormat = jsonFormat1(ModelUpdated.apply)

  implicit val modelDeletedFormat = jsonFormat1(ModelDeleted.apply)

  implicit val indexErrorFormat = new JsonFormat[IndexError] {
    override def write(obj: IndexError): JsValue =
      JsObject(Map(
        "model" -> obj.model.toJson,
        "error" -> JsString(obj.error.getMessage)
      ))

    override def read(json: JsValue): IndexError = {
      json match {
        case JsObject(fields) if fields.isDefinedAt("model") && fields.isDefinedAt("error") =>
          IndexError(
            fields("model").convertTo[Model],
            new IllegalArgumentException(fields("error").convertTo[String])
          )
        case x => throw new DeserializationException(s"Can't read IndexError: $x")
      }
    }
  }

  implicit val indexStatusFormat = new JsonFormat[IndexStatus]{
    override def write(obj: IndexStatus): JsValue = {
      val serialized = obj match {
        case x: ModelUpdated => x.toJson
        case x: ModelDeleted => x.toJson
        case x: IndexError => x.toJson
      }
      JsObject(serialized.asJsObject.fields + ("type" -> JsString(obj.productPrefix)))
    }

    override def read(json: JsValue): IndexStatus = {
      json match {
        case JsObject(fields) if fields.isDefinedAt("type") =>
          fields("type") match {
            case JsString("ModelUpdated") =>
              json.convertTo[ModelUpdated]
            case JsString("ModelDeleted") =>
              json.convertTo[ModelDeleted]
            case JsString("IndexError") =>
              json.convertTo[IndexError]
            case x => throw DeserializationException(s"Unknown IndexStatus type: $x")
          }
        case x => throw DeserializationException(s"Can't read IndexStatus: $x")
      }
    }
  }

  implicit def internalErrorFormat[T <: Throwable] = new RootJsonFormat[InternalError[T]] {
    override def write(obj: InternalError[T]): JsValue = {
      val fields = Map(
        "exception" -> JsString(obj.exception.getMessage)
      )
      val reasonField = obj.reason.map { r =>
        Map("reason" -> JsString(r))
      }.getOrElse(Map.empty)

      JsObject(fields ++ reasonField)
    }

    override def read(json: JsValue): InternalError[T] = ???
  }

  implicit val clientErrorFormat = jsonFormat1(ClientError.apply)

  implicit val errorFormat = new RootJsonFormat[HError] {
    override def write(obj: HError): JsValue = {
      obj match {
        case x: ClientError => JsObject(Map(
          "error" -> JsString("Client"),
          "information" -> x.toJson
        ))
        case x: InternalError[_] => JsObject(Map(
          "error" -> JsString("Internal"),
          "information" -> x.toJson
        ))
        case ErrorCollection(errors) => JsObject(Map(
          "error" -> JsString("Multiple"),
          "information" -> JsArray(errors.map(write).toVector)
        ))
      }
    }

    override def read(json: JsValue): HError = ???
  }
}

object CompleteJsonProtocol extends CompleteJsonProtocol

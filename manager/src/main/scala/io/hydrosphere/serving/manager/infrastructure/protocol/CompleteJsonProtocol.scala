package io.hydrosphere.serving.manager.infrastructure.protocol

import io.hydrosphere.serving.manager.api.http.controller.environment.CreateEnvironmentRequest
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.clouddriver.{MetricServiceTargetLabels, MetricServiceTargets}
import io.hydrosphere.serving.manager.domain.model.UpdateModelRequest
import io.hydrosphere.serving.manager.domain.model_version.ModelVersionView
import io.hydrosphere.serving.model.api.Result.{ClientError, ErrorCollection, HError, InternalError}
import spray.json.{JsObject, JsString, JsValue, _}

trait CompleteJsonProtocol extends CommonJsonProtocol with ContractJsonProtocol with ModelJsonProtocol {

  implicit val updateModelRequest = jsonFormat2(UpdateModelRequest)

  implicit val createEnvironmentRequest = jsonFormat2(CreateEnvironmentRequest)

  implicit val metricServiceTargetLabelsFormat = jsonFormat11(MetricServiceTargetLabels)

  implicit val metricServiceTargetsFormat = jsonFormat2(MetricServiceTargets)

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

  implicit val modelUpload = jsonFormat7(ModelUploadMetadata.apply)

  implicit val versionView = jsonFormat12(ModelVersionView.apply)
}

object CompleteJsonProtocol extends CompleteJsonProtocol

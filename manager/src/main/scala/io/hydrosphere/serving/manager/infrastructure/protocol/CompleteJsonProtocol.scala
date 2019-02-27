package io.hydrosphere.serving.manager.infrastructure.protocol

import io.hydrosphere.serving.manager.api.http.controller.host_selector.CreateHostSelector
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.DomainError
import io.hydrosphere.serving.manager.domain.clouddriver.{MetricServiceTargetLabels, MetricServiceTargets}
import io.hydrosphere.serving.manager.domain.model_version.ModelVersionView
import spray.json._

trait CompleteJsonProtocol extends CommonJsonProtocol with ContractJsonProtocol with ModelJsonProtocol {

  implicit val createEnvironmentRequest = jsonFormat2(CreateHostSelector)

  implicit val metricServiceTargetLabelsFormat = jsonFormat11(MetricServiceTargetLabels)

  implicit val metricServiceTargetsFormat = jsonFormat2(MetricServiceTargets)

  implicit val errorFormat = new RootJsonFormat[DomainError] {
    override def write(obj: DomainError): JsValue = {
      obj match {
        case x: DomainError.NotFound => JsObject(Map(
          "error" -> JsString("NotFound"),
          "message" -> JsString(x.message)
        ))
        case x: DomainError.InvalidRequest => JsObject(Map(
          "error" -> JsString("InvalidRequest"),
          "message" -> JsString(x.message)
        ))
        case x: DomainError.InternalError => JsObject(Map(
          "error" -> JsString("InternalError"),
          "information" -> JsString(x.message)
        ))
        case x => JsObject(Map(
          "error" -> JsString("DomainError"),
          "information" -> JsString(x.message)
        ))
      }
    }

    override def read(json: JsValue): DomainError = throw DeserializationException("Can't deserealize DomainError")
  }

  implicit val modelUpload = jsonFormat7(ModelUploadMetadata.apply)

  implicit val versionView = jsonFormat12(ModelVersionView.apply)
}

object CompleteJsonProtocol extends CompleteJsonProtocol

package io.hydrosphere.serving.manager.model.protocol

import io.hydrosphere.serving.manager.controller.environment.CreateEnvironmentRequest
import io.hydrosphere.serving.manager.controller.model.AddModelRequest
import io.hydrosphere.serving.manager.model.Result.{ClientError, ErrorCollection, HError, InternalError}
import io.hydrosphere.serving.manager.service.aggregated_info.{AggregatedModelBuild, AggregatedModelInfo, AggregatedModelVersion}
import io.hydrosphere.serving.manager.service.clouddriver.{MetricServiceTargetLabels, MetricServiceTargets}
import io.hydrosphere.serving.manager.service.model._
import io.hydrosphere.serving.manager.service.model_version.CreateModelVersionRequest
import io.hydrosphere.serving.manager.service.runtime.{ServiceTaskRegistered, ServiceTaskStatus, _}
import io.hydrosphere.serving.manager.service.service.CreateServiceRequest
import spray.json.{JsObject, JsString, JsValue, _}

trait CompleteJsonProtocol extends CommonJsonProtocol with ContractJsonProtocol with ModelJsonProtocol {
  implicit val serviceTaskRegistered = new RootJsonFormat[ServiceTaskRegistered.type]{
    override def read(json: JsValue) = json match {
      case JsString("Registered") => ServiceTaskRegistered
      case x => throw DeserializationException(s"Unknown ServiceTaskRegistered: $x")
    }

    override def write(obj: ServiceTaskRegistered.type): JsValue = JsString("Registered")
  }
  implicit val serviceTaskInProgress = new RootJsonFormat[ServiceTaskInProgress.type]{
    override def read(json: JsValue) = json match {
      case JsString("Running") => ServiceTaskInProgress
      case x => throw DeserializationException(s"Unknown ServiceTaskInProgress: $x")
    }

    override def write(obj: ServiceTaskInProgress.type): JsValue = JsString("Running")
  }
  implicit val serviceTaskFinished = new RootJsonFormat[ServiceTaskFinished.type]{
    override def read(json: JsValue) = json match {
      case JsString("Finished") => ServiceTaskFinished
      case x => throw DeserializationException(s"Unknown ServiceTaskFinished: $x")
    }

    override def write(obj: ServiceTaskFinished.type): JsValue = JsString("Finished")
  }
  implicit val serviceTaskFailed = new RootJsonFormat[ServiceTaskFailed.type]{
    override def read(json: JsValue) = json match {
      case JsString("Failed") => ServiceTaskFailed
      case x => throw DeserializationException(s"Unknown ServiceTaskFailed: $x")
    }

    override def write(obj: ServiceTaskFailed.type): JsValue = JsString("Failed")
  }
  implicit val serviceTask = new RootJsonFormat[ServiceTaskStatus]{
    override def read(json: JsValue): ServiceTaskStatus = json match {
      case JsString("Failed") => ServiceTaskFailed
      case JsString("Registered") => ServiceTaskRegistered
      case JsString("Running") => ServiceTaskInProgress
      case JsString("Finished") => ServiceTaskFinished

      case x => throw DeserializationException(s"Unknown ServiceTaskStatus: $x")
    }

    override def write(obj: ServiceTaskStatus): JsValue = JsString("Failed")
  }

  implicit val createServiceRequest = jsonFormat5(CreateServiceRequest)

  implicit val createRuntimeRequest = jsonFormat5(CreateRuntimeRequest)
  implicit val createRuntimeInProgress = jsonFormat4(RuntimeCreationInProgress.apply)
  implicit val runtimeFailed = jsonFormat6(RuntimeCreationFailed.apply)
  implicit val runtimeCreated = jsonFormat6(RuntimeCreated.apply)

  implicit val createRuntimeStatus = new RootJsonWriter[RuntimeCreateStatus] {
    override def write(obj: RuntimeCreateStatus): JsValue = {
      obj match {
        case x: RuntimeCreationInProgress => x.toJson
        case x: RuntimeCreationFailed => x.toJson
        case x: RuntimeCreated => x.toJson
      }
    }
  }

  implicit val createModelRequest = jsonFormat4(CreateModelRequest)

  implicit val updateModelRequest = jsonFormat5(UpdateModelRequest)

  implicit val createModelVersionRequest = jsonFormat12(CreateModelVersionRequest)

  implicit val createEnvironmentRequest = jsonFormat2(CreateEnvironmentRequest)

  implicit val addModelFormat = jsonFormat2(AddModelRequest.apply)

  implicit val aggregatedModelInfoFormat = jsonFormat4(AggregatedModelInfo)

  implicit val aggregatedModelVersionFormat = jsonFormat11(AggregatedModelVersion.apply)

  implicit val aggregatedModelBuildFormat = jsonFormat9(AggregatedModelBuild.apply)

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
}

object CompleteJsonProtocol extends CompleteJsonProtocol

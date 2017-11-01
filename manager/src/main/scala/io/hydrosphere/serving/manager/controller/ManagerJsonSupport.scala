package io.hydrosphere.serving.manager.controller

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.model._
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}
import spray.json._
/**
  *
  */
trait ManagerJsonSupport extends CommonJsonSupport {
  implicit val modelBuildStatusFormat = new EnumJsonConverter(ModelBuildStatus)
  implicit val modelServiceInstanceStatusFormat = new EnumJsonConverter(ModelServiceInstanceStatus)

  implicit val modelFormat = jsonFormat9(Model)

  implicit val buildModelRequestFormat = jsonFormat2(BuildModelRequest)
  implicit val buildModelByNameRequest = jsonFormat2(BuildModelByNameRequest)

  implicit val modelBuildFormat = jsonFormat9(ModelBuild)

  implicit val modelServiceInstanceFormat = jsonFormat8(ModelServiceInstance)

  implicit val createModelServiceRequest = jsonFormat3(CreateModelServiceRequest)

  implicit val createRuntimeTypeRequest = jsonFormat4(CreateRuntimeTypeRequest)

  implicit val createOrUpdateModelRequest = jsonFormat7(CreateOrUpdateModelRequest)

  implicit val createModelRuntime = jsonFormat12(CreateModelRuntime)

  implicit val updateModelRuntime = jsonFormat7(UpdateModelRuntime)

  implicit val serveData = jsonFormat3(ServeData)

  implicit val applicationCreateOrUpdateRequest = jsonFormat4(ApplicationCreateOrUpdateRequest)

  implicit val localModelFormat = jsonFormat3(LocalModelSourceEntry.apply)
  implicit val s3ModelFormat = jsonFormat7(S3ModelSourceEntry.apply)

  implicit object ModelSourceJsonFormat extends RootJsonFormat[ModelSource] {
    def write(a: ModelSource) = a match {
      case p: LocalModelSourceEntry => p.toJson
      case p: S3ModelSourceEntry => p.toJson
    }

    def read(value: JsValue) = {
      val keys = value.asJsObject.fields.keys
      if (keys.exists(_ == JsString("bucketName"))) {
        value.convertTo[S3ModelSourceEntry]
      } else if (keys.exists(_ == JsString("path"))) {
        value.convertTo[LocalModelSourceEntry]
      } else {
        throw DeserializationException(s"$value is not a correct ModelSource")
      }
    }
  }
}

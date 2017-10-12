package io.hydrosphere.serving.manager.controller

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service.modelfetcher.{FieldType, ModelField}
import io.hydrosphere.serving.manager.service.{WeightedServiceCreateOrUpdateRequest, _}
import io.hydrosphere.serving.model._
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}
import spray.json._
/**
  *
  */
trait ManagerJsonSupport extends CommonJsonSupport {
  implicit object FieldTypeFormat extends RootJsonFormat[FieldType] {
    override def read(json: JsValue): FieldType = json match {
      case JsString("integer") => FieldType.FInteger
      case JsString("float") => FieldType.FFloat
      case JsObject(field) if field.get("type").isDefined && field("type") == JsString("list") =>
        FieldType.FList(field("item_type").convertTo[FieldType], field("size").convertTo[Long])
      case _ => throw DeserializationException(s"$json is not a valid field type definition.")
    }

    override def write(obj: FieldType): JsValue = obj match {
      case FieldType.FInteger => JsString("integer")
      case FieldType.FFloat => JsString("float")
      case FieldType.FList(fType, size) =>
        val s = Map(
          "size" -> JsNumber(size),
          "item_type" -> fType.toJson,
          "type" -> JsString("list")
        )
        JsObject(s)
    }
  }

  implicit val typedFieldFormat = jsonFormat2(ModelField.TypedField)
  implicit object ModelFieldFormat extends RootJsonFormat[ModelField] {
    def write(a: ModelField) = a match {
      case p: ModelField.UntypedField => JsString(p.name)
      case p: ModelField.TypedField => p.toJson
    }

    def read(value: JsValue) = value match {
      case JsString(name) => ModelField.UntypedField(name)
      case obj: JsObject => obj.convertTo[ModelField.TypedField]
      case _ => throw DeserializationException(s"$value is not a valid model field definition.")
    }
  }


  implicit val modelBuildStatusFormat = new EnumJsonConverter(ModelBuildStatus)
  implicit val modelServiceInstanceStatusFormat = new EnumJsonConverter(ModelServiceInstanceStatus)

  implicit val modelFormat = jsonFormat9(Model)

  implicit val buildModelRequestFormat = jsonFormat2(BuildModelRequest)
  implicit val buildModelByNameRequest = jsonFormat2(BuildModelByNameRequest)

  implicit val modelBuildFormat = jsonFormat9(ModelBuild)

  implicit val modelServiceInstanceFormat = jsonFormat8(ModelServiceInstance)

  implicit val createEndpointRequest = jsonFormat2(CreateEndpointRequest)

  implicit val createModelServiceRequest = jsonFormat2(CreateModelServiceRequest)

  implicit val createRuntimeTypeRequest = jsonFormat3(CreateRuntimeTypeRequest)

  implicit val createOrUpdateModelRequest = jsonFormat7(CreateOrUpdateModelRequest)

  implicit val createModelRuntime = jsonFormat10(CreateModelRuntime)

  implicit val updateModelRuntime = jsonFormat7(UpdateModelRuntime)

  implicit val createPipelineStageRequest = jsonFormat2(CreatePipelineStageRequest)

  implicit val createPipelineRequest = jsonFormat2(CreatePipelineRequest)

  implicit val modelInfoFormat = jsonFormat4(ModelInfo)

  implicit val serveData = jsonFormat3(ServeData)

  implicit val weightedServiceCreateOrUpdateRequest = jsonFormat3(WeightedServiceCreateOrUpdateRequest)

  implicit val localModelFormat = jsonFormat3(LocalModelSource.apply)
  implicit val s3ModelFormat = jsonFormat7(S3ModelSource.apply)

  implicit object ModelSourceJsonFormat extends RootJsonFormat[ModelSource] {
    def write(a: ModelSource) = a match {
      case p: LocalModelSource => p.toJson
      case p: S3ModelSource => p.toJson
    }

    def read(value: JsValue) = {
      val keys = value.asJsObject.fields.keys
      if (keys.exists(_ == JsString("bucketName"))) {
        value.convertTo[S3ModelSource]
      } else if (keys.exists(_ == JsString("path"))) {
        value.convertTo[LocalModelSource]
      } else {
        throw DeserializationException(s"$value is not a correct ModelSource")
      }
    }
  }
}

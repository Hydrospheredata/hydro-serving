package io.hydrosphere.serving.manager.infrastructure.protocol

import io.hydrosphere.serving.manager.domain.application._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionStatus}
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableStatus}
import io.hydrosphere.serving.manager.domain.servable.Servable
import spray.json._


trait ModelJsonProtocol extends CommonJsonProtocol with ContractJsonProtocol {
  
  implicit val servableStatus = new RootJsonFormat[ServableStatus] {
    
    implicit val running = jsonFormat2(ServableStatus.Running.apply)
  
    object Keys {
      val Running = "running"
      val Starting = "starting"
      val Stopped = "stopped"
    }
    
    override def read(json: JsValue): ServableStatus = {
      val obj = json.asJsObject
      obj.fields.get("type") match {
        case Some(JsString(x)) => x match {
          case Keys.Running => running.read(obj)
          case Keys.Starting => ServableStatus.Starting
          case Keys.Stopped => ServableStatus.Stopped
          case x => throw new DeserializationException(s"Invalid type field: $x")
        }
        case x => throw new DeserializationException(s"Invalid type field: $x")
      }
    }
    
    override def write(obj: ServableStatus): JsValue = {
      obj  match {
        case ServableStatus.Starting => JsObject("type" -> JsString(Keys.Starting))
        case r: ServableStatus.Running =>
          val body = running.write(r).asJsObject
          val fields = body.fields + ("type" -> JsString(Keys.Running))
          JsObject(fields)
        case ServableStatus.Stopped => JsObject("type" -> JsString(Keys.Stopped))
      }
    }
  }

  implicit val dockerImageFormat = jsonFormat3(DockerImage.apply)

  implicit val modelFormat = jsonFormat2(Model)
  implicit val environmentFormat = jsonFormat3(HostSelector)
  implicit val versionStatusFormat = enumFormat(ModelVersionStatus)
  implicit val modelVersionFormat = jsonFormat13(ModelVersion.apply)
  implicit val serviceFormat = jsonFormat3(Servable.apply)

  implicit val detailedServiceFormat = jsonFormat2(ModelVariant.apply)
  implicit val applicationStageFormat = jsonFormat2(PipelineStage.apply)
  implicit val applicationExecutionGraphFormat = jsonFormat1(ApplicationExecutionGraph)
  implicit val applicationKafkaStreamingFormat = jsonFormat4(ApplicationKafkaStream)
  implicit val appStatusFormat = enumFormat(ApplicationStatus)
  implicit val applicationFormat = jsonFormat7(Application.apply)
}

object ModelJsonProtocol extends ModelJsonProtocol
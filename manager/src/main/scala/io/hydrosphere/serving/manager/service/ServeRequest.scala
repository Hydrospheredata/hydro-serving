package io.hydrosphere.serving.manager.service

import akka.http.scaladsl.model.HttpHeader
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import spray.json.JsObject

import scala.util.{Failure, Success, Try}

sealed trait ServiceKey
case class ApplicationKey(id: Long) extends ServiceKey
case class ApplicationName(name: String) extends ServiceKey

sealed trait ModelKey extends ServiceKey
case class ModelById(id: Long) extends ModelKey
case class ModelByName(name: String, version: Option[Long] = None) extends ModelKey

case class ServeRequest(
  serviceKey: ServiceKey,
  servePath: String,
  headers: Seq[HttpHeader],
  inputData: Array[Byte]
)

case class JsonPredictRequest(
  modelName: String,
  version: Option[Long],
  signatureName: String,
  inputs: JsObject
) {
  def toModelSpec: ModelSpec = ModelSpec(modelName, version, signatureName)
}

object JsonPredictRequest{
  import spray.json._
  def fromServeRequest(serveRequest: ServeRequest): Try[JsonPredictRequest] = {
    serveRequest.serviceKey match {
      case ModelByName(name, version) =>
        Success(
          JsonPredictRequest(
            modelName = name,
            version = version,
            signatureName = serveRequest.servePath,
            inputs = serveRequest.inputData.toString.parseJson.asJsObject
          )
        )
      case _ => Failure(new IllegalArgumentException("Only 'ModelByName' is supported as 'serviceKey'"))
    }
  }
}
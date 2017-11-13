package io.hydrosphere.serving.manager.service

import akka.http.scaladsl.model.HttpHeader

sealed trait ServiceKey

case class ApplicationKey(id: Long) extends ServiceKey
case class ApplicationName(name: String) extends ServiceKey

sealed trait ModelKey extends ServiceKey
case class ModelById(id: Long) extends ModelKey
case class ModelByName(name: String, version: Option[String] = None) extends ModelKey

case class ServeRequest(
  serviceKey: ServiceKey,
  servePath: String,
  headers: Seq[HttpHeader],
  inputData: Array[Byte]
)
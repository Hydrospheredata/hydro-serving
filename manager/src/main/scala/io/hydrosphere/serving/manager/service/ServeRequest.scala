package io.hydrosphere.serving.manager.service

sealed trait ServeKey

case class ApplicationKey(id: Long) extends ServeKey

case class ApplicationName(name: String) extends ServeKey

case class ServiceById(id: Long) extends ServeKey

case class ServeRequest(
  serviceKey: ServeKey,
  inputData: Array[Byte]
)
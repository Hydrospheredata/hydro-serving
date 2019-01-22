package io.hydrosphere.serving.manager.domain.application

object ApplicationStatus extends Enumeration {
  type ApplicationStatus = Value
  val Assembling, Ready, Failed = Value
}
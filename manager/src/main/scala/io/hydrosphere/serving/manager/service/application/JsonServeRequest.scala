package io.hydrosphere.serving.manager.service.application

import spray.json.JsObject

case class JsonServeRequest(
  targetId: Long,
  signatureName: String,
  inputs: JsObject
)

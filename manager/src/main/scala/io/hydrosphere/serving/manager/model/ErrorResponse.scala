package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class ErrorResponse(
  message: String
)

object ErrorResponse {
  implicit val errorResponseFormat = jsonFormat1(ErrorResponse.apply)
}
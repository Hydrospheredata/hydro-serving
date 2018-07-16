package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.model.DataProfileType.DataProfileType
import io.hydrosphere.serving.manager.model.Result.HError

import scala.concurrent.Future

package object model {
  type HResult[T] = Either[HError, T]
  type HFResult[T] = Future[Either[HError, T]]

  type DataProfileFields = Map[String, DataProfileType]
}

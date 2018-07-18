package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.model.Result.HError
import io.hydrosphere.serving.monitoring.data_profile_types.DataProfileType

import scala.concurrent.Future

package object model {
  type HResult[T] = Either[HError, T]
  type HFResult[T] = Future[Either[HError, T]]

  type DataProfileFields = Map[String, DataProfileType]
}

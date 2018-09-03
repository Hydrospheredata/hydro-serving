package io.hydrosphere.serving.manager

import io.hydrosphere.serving.monitoring.data_profile_types.DataProfileType

package object model {
  type DataProfileFields = Map[String, DataProfileType]
}
